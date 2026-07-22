package com.hospital.noshow.repository;

import com.hospital.noshow.entity.RiskScore;
import com.hospital.noshow.enums.AppointmentStatus;
import com.hospital.noshow.enums.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RiskScoreRepository extends JpaRepository<RiskScore, Long> {

    Optional<RiskScore> findByAppointmentId(Long appointmentId);

    List<RiskScore> findByRiskLevel(RiskLevel riskLevel);

    // §11 Job 2: today's HIGH-risk appointments for the morning digest
    List<RiskScore> findByRiskLevelAndAppointment_ScheduledDate(RiskLevel riskLevel, LocalDate date);

    // §11 Job 3: weekly accuracy report — predicted-HIGH count
    long countByRiskLevelAndScoredAtAfter(RiskLevel riskLevel, LocalDateTime since);

    // §11 Job 3: weekly accuracy report — true positives (predicted HIGH, actually NO_SHOW)
    long countByRiskLevelAndAppointment_Status(RiskLevel riskLevel, AppointmentStatus status);

    long countByRiskLevel(RiskLevel riskLevel);
}