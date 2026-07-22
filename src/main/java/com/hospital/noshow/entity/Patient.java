package com.hospital.noshow.entity;

import com.hospital.noshow.enums.Gender;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int age;

    // Now an enum backed by ENUM('M','F','OTHER') in §4 — invalid values
    // can no longer be persisted, and §13's encode_gender('F'/'M'/other)
    // now has a closed set of inputs to branch on instead of free text.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Column(length = 15)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(nullable = false)
    private boolean scholarship = false;

    @Column(nullable = false)
    private boolean hypertension = false;

    @Column(nullable = false)
    private boolean diabetes = false;

    @Column(nullable = false)
    private boolean alcoholism = false;

    @Column(name = "sms_received", nullable = false)
    private boolean smsReceived = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}