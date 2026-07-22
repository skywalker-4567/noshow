package com.hospital.noshow.scheduler;

import com.hospital.noshow.entity.Appointment;
import com.hospital.noshow.entity.RiskScore;
import com.hospital.noshow.entity.User;
import com.hospital.noshow.enums.AppointmentStatus;
import com.hospital.noshow.enums.RiskLevel;
import com.hospital.noshow.enums.UserRole;
import com.hospital.noshow.repository.AppointmentRepository;
import com.hospital.noshow.repository.RiskScoreRepository;
import com.hospital.noshow.repository.UserRepository;
import com.hospital.noshow.service.EmailService;
import com.hospital.noshow.service.RiskScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.hospital.noshow.exception.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class HospitalScheduler {

    private final AppointmentRepository appointmentRepository;
    private final RiskScoringService riskScoringService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final RiskScoreRepository riskScoreRepository;

    // ── Job 1: Nightly Batch Scoring ─────────────────────────────────────────
    @Scheduled(cron = "0 0 21 * * *")
    public void runNightlyBatchScoring() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Appointment> appointments = appointmentRepository.findByScheduledDate(tomorrow);

        int scored = 0, failed = 0;
        for (Appointment appt : appointments) {
            try {
                riskScoringService.scoreAndPersist(appt);   // @Transactional on THIS method
                scored++;
            } catch (Exception e) {
                // Per-record failure — batch continues
                log.error("Scoring failed for appointment {}: {}", appt.getId(), e.getMessage());
                failed++;
            }
        }
        log.info("Batch scoring complete. Scored={}, Failed={}", scored, failed);
    }

    // ── Job 2: Morning Risk Alerts ────────────────────────────────────────────
    @Scheduled(cron = "0 0 8 * * *")
    public void sendMorningRiskAlerts() {
        LocalDate today = LocalDate.now();
        List<RiskScore> highRiskToday = riskScoreRepository
                .findByRiskLevelAndAppointment_ScheduledDate(RiskLevel.HIGH, today);

        if (!highRiskToday.isEmpty()) {
            List<String> receptionistEmails = userRepository.findByRole(UserRole.RECEPTIONIST)
                    .stream().map(User::getEmail).collect(Collectors.toList());
            emailService.sendMorningRiskDigest(receptionistEmails, highRiskToday);
        }
        log.info("Morning alerts: {} HIGH-risk appointments today", highRiskToday.size());
    }

    // ── Job 3: Weekly Accuracy Report ────────────────────────────────────────
    @Scheduled(cron = "0 0 7 * * MON")
    public void generateWeeklyAccuracyReport() {
        LocalDate weekAgo = LocalDate.now().minusDays(7);
        long predictedHigh = riskScoreRepository
                .countByRiskLevelAndScoredAtAfter(RiskLevel.HIGH, weekAgo.atStartOfDay());
        long actualNoShow = appointmentRepository
                .countByStatusAndScheduledDateAfter(AppointmentStatus.NO_SHOW, weekAgo);
        long truePositives = riskScoreRepository
                .countByRiskLevelAndAppointment_Status(RiskLevel.HIGH, AppointmentStatus.NO_SHOW);

        String adminEmail = userRepository.findByRole(UserRole.ADMIN).stream()
                .findFirst()
                .map(User::getEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No ADMIN user found — cannot determine recipient for weekly accuracy report."));

        emailService.sendWeeklyAccuracyReport(adminEmail, predictedHigh, actualNoShow, truePositives);
    }
}