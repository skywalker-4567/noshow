package com.hospital.noshow.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "doctors")
@Data
@NoArgsConstructor
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String specialization;

    // §5: User (1) ──── (0..1) Doctor — owning side holds the unique FK
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;
}