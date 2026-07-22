package com.hospital.noshow.service;

import com.hospital.noshow.entity.Appointment;
import com.hospital.noshow.entity.RiskScore;
import com.hospital.noshow.enums.UserRole;
import com.hospital.noshow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Async("emailTaskExecutor")
    @Override
    public void sendAppointmentConfirmation(Appointment appt) {
        String to = appt.getPatient().getEmail();
        if (to == null || to.isBlank()) {
            log.warn("No email on file for patient #{} — skipping confirmation send.", appt.getPatient().getId());
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Appointment Confirmed — #" + appt.getId());
        msg.setText(String.format(
                "Hi %s,%n%nYour appointment with Dr. %s is scheduled for %s at %s.%n%nThank you.",
                appt.getPatient().getName(), appt.getDoctor().getName(),
                appt.getScheduledDate(), appt.getScheduledTime()));
        mailSender.send(msg);
    }

    @Async("emailTaskExecutor")
    @Override
    public void sendStatusUpdateToPatient(Appointment appt) {
        String to = appt.getPatient().getEmail();
        if (to == null || to.isBlank()) {
            log.warn("No email on file for patient #{} — skipping status update send.", appt.getPatient().getId());
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Appointment Status Update — #" + appt.getId());
        msg.setText(String.format(
                "Hi %s,%n%nYour appointment scheduled for %s at %s is now: %s.",
                appt.getPatient().getName(), appt.getScheduledDate(),
                appt.getScheduledTime(), appt.getStatus()));
        mailSender.send(msg);
    }

    @Async("emailTaskExecutor")
    @Override
    public void sendNoShowNotification(Appointment appt) {
        List<String> adminEmails = userRepository.findByRole(UserRole.ADMIN)
                .stream().map(u -> u.getEmail()).toList();
        if (adminEmails.isEmpty()) {
            log.warn("No ADMIN users found — skipping no-show notification for appointment #{}.", appt.getId());
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(adminEmails.toArray(new String[0]));
        msg.setSubject("No-Show Recorded — Appointment #" + appt.getId());
        msg.setText(String.format(
                "Patient %s missed their appointment on %s at %s with Dr. %s.",
                appt.getPatient().getName(), appt.getScheduledDate(),
                appt.getScheduledTime(), appt.getDoctor().getName()));
        mailSender.send(msg);
    }

    @Async("emailTaskExecutor")
    @Override
    public void sendHighRiskAlert(Appointment appt, RiskScore score) {
        List<String> receptionistEmails = userRepository.findByRole(UserRole.RECEPTIONIST)
                .stream().map(u -> u.getEmail()).toList();
        if (receptionistEmails.isEmpty()) {
            log.warn("No RECEPTIONIST users found — skipping HIGH-risk alert for appointment #{}.", appt.getId());
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(receptionistEmails.toArray(new String[0]));
        msg.setSubject("HIGH Risk Appointment — #" + appt.getId());
        msg.setText(String.format(
                "Appointment #%d for patient %s on %s at %s is flagged HIGH risk (score=%.4f). Consider a reminder call.",
                appt.getId(), appt.getPatient().getName(), appt.getScheduledDate(),
                appt.getScheduledTime(), score.getRiskScore()));
        mailSender.send(msg);
    }

    @Async("emailTaskExecutor")
    @Override
    public void sendMorningRiskDigest(List<String> recipientEmails, List<RiskScore> highRiskScores) {
        if (recipientEmails.isEmpty()) {
            log.warn("No recipients supplied for morning risk digest — skipping.");
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(recipientEmails.toArray(new String[0]));
        msg.setSubject("Morning Risk Digest — " + highRiskScores.size() + " HIGH-risk appointment(s) today");
        StringBuilder body = new StringBuilder("HIGH-risk appointments scheduled today:\n\n");
        for (RiskScore rs : highRiskScores) {
            body.append(String.format("- Appointment #%d, patient %s, score %.4f%n",
                    rs.getAppointment().getId(), rs.getAppointment().getPatient().getName(), rs.getRiskScore()));
        }
        msg.setText(body.toString());
        mailSender.send(msg);
    }

    @Async("emailTaskExecutor")
    @Override
    public void sendWeeklyAccuracyReport(String adminEmail, long predictedHigh, long actualNoShow, long truePositives) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("No admin email supplied for weekly accuracy report — skipping.");
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(adminEmail);
        msg.setSubject("Weekly No-Show Risk Accuracy Report");
        msg.setText(String.format(
                "Past 7 days:%n- Predicted HIGH risk: %d%n- Actual no-shows: %d%n- True positives (predicted HIGH, actually no-show): %d",
                predictedHigh, actualNoShow, truePositives));
        mailSender.send(msg);
    }
}