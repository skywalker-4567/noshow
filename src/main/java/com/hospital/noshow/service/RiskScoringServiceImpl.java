package com.hospital.noshow.service;

import com.hospital.noshow.dto.RiskPredictionRequest;
import com.hospital.noshow.dto.RiskPredictionResponse;
import com.hospital.noshow.entity.Appointment;
import com.hospital.noshow.entity.Patient;
import com.hospital.noshow.entity.RiskScore;
import com.hospital.noshow.enums.RiskLevel;
import com.hospital.noshow.repository.RiskScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskScoringServiceImpl implements RiskScoringService {

    private final RestTemplate restTemplate;
    private final RiskScoreRepository riskScoreRepository;
    private final AuditService auditService;
    private final EmailService emailService;

    @Value("${flask.predict.url}")
    private String flaskPredictUrl;

    @Transactional   // one transaction per appointment — score save + audit log are atomic
    @Override
    public void scoreAndPersist(Appointment appt) {
        if (riskScoreRepository.findByAppointmentId(appt.getId()).isPresent()) {
            log.info("Appointment {} already scored — skipping.", appt.getId());
            return;
        }
        int daysWaiting = Math.max(0, (int) ChronoUnit.DAYS.between(
                appt.getCreatedAt().toLocalDate(), appt.getScheduledDate()));
        // Math.max(0, ...) guards against same-day or backdated bookings in test data

        RiskPredictionRequest req = buildRequest(appt, daysWaiting);
        RiskPredictionResponse resp;

        try {
            resp = scoreViaFlask(req);
        } catch (RestClientException e) {
            log.warn("Flask unreachable for appointment {}. Applying fallback rules.", appt.getId());
            resp = applyFallbackRules(appt, daysWaiting);
        }

        RiskScore rs = new RiskScore();
        rs.setAppointment(appt);
        rs.setRiskScore(BigDecimal.valueOf(resp.getRiskScore()));
        rs.setRiskLevel(RiskLevel.valueOf(resp.getRiskLevel()));
        rs.setDaysWaiting(daysWaiting);
        rs.setScoredAt(LocalDateTime.now());
        rs.setModelVersion(resp.getModelVersion());
        riskScoreRepository.save(rs);  // write 1

        auditService.writeLog(                         // write 2 — both or neither
                "APPOINTMENT", appt.getId(),
                appt.getStatus().name(), "RISK_SCORED", "SCHEDULER");

        // @Async email fires AFTER transaction commits — correct behavior
        if (rs.getRiskLevel() == RiskLevel.HIGH) {
            emailService.sendHighRiskAlert(appt, rs);
        }
    }

    private RiskPredictionRequest buildRequest(Appointment appt, int daysWaiting) {
        Patient p = appt.getPatient();
        RiskPredictionRequest req = new RiskPredictionRequest();
        req.setAge(p.getAge());
        req.setGender(p.getGender().name());   // Gender enum -> "M"/"F"/"OTHER" string
        req.setScholarship(p.isScholarship() ? 1 : 0);
        req.setHypertension(p.isHypertension() ? 1 : 0);
        req.setDiabetes(p.isDiabetes() ? 1 : 0);
        req.setAlcoholism(p.isAlcoholism() ? 1 : 0);
        req.setSmsReceived(p.isSmsReceived() ? 1 : 0);
        req.setDaysWaiting(daysWaiting);
        return req;
    }

    private RiskPredictionResponse scoreViaFlask(RiskPredictionRequest request) {
        return restTemplate.postForObject(
                flaskPredictUrl,
                request,
                RiskPredictionResponse.class
        );
        // Throws RestClientException if Flask is unreachable.
        // Caller (scoreAndPersist) catches it and applies fallback.
    }

    private RiskPredictionResponse applyFallbackRules(Appointment appt, int daysWaiting) {
        Patient p = appt.getPatient();
        RiskLevel level;
        if (daysWaiting > 15 && !p.isSmsReceived() && (p.isHypertension() || p.isDiabetes())) {
            level = RiskLevel.HIGH;
        } else if (daysWaiting > 7) {
            level = RiskLevel.MEDIUM;
        } else {
            level = RiskLevel.LOW;
        }
        RiskPredictionResponse fallback = new RiskPredictionResponse();
        fallback.setRiskLevel(level.name());
        fallback.setRiskScore(level == RiskLevel.HIGH ? 0.65 :
                level == RiskLevel.MEDIUM ? 0.40 : 0.15);
        fallback.setModelVersion("fallback-rules");
        return fallback;
    }
}