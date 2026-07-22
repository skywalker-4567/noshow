package com.hospital.noshow.service;

import com.hospital.noshow.entity.Appointment;
import com.hospital.noshow.entity.RiskScore;

import java.util.List;

public interface EmailService {

    // §12 trigger table — concrete @Async implementations arrive in item 11
    void sendAppointmentConfirmation(Appointment appt);

    void sendStatusUpdateToPatient(Appointment appt);

    void sendNoShowNotification(Appointment appt);

    void sendHighRiskAlert(Appointment appt, RiskScore score);

    void sendMorningRiskDigest(List<String> recipientEmails, List<RiskScore> highRiskScores);

    void sendWeeklyAccuracyReport(String adminEmail, long predictedHigh, long actualNoShow, long truePositives);
}