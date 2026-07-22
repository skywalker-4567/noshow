package com.hospital.noshow.entity;

import com.hospital.noshow.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_scores")
@Data
@NoArgsConstructor
public class RiskScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // §5: Appointment (1) ──── (0..1) RiskScore
    @OneToOne
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(name = "risk_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "days_waiting", nullable = false)
    private int daysWaiting;

    @Column(name = "scored_at")
    private LocalDateTime scoredAt;

    @Column(name = "model_version", length = 20)
    private String modelVersion = "1.0";

    @PrePersist
    protected void onCreate() {
        if (this.scoredAt == null) {
            this.scoredAt = LocalDateTime.now();
        }
    }
}