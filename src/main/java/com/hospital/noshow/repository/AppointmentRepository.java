package com.hospital.noshow.repository;

import com.hospital.noshow.entity.Appointment;
import com.hospital.noshow.entity.Doctor;
import com.hospital.noshow.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // §11 Job 1: nightly batch scoring pulls tomorrow's appointments
    List<Appointment> findByScheduledDate(LocalDate date);

    // §9: base pagination used on /admin/appointments
    Page<Appointment> findAll(Pageable pageable);

    // §9: status-filtered pagination
    Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable);

    Page<Appointment> findByScheduledDate(LocalDate date, Pageable pageable);

    // §11 Job 3: weekly accuracy report — actual no-show count
    long countByStatusAndScheduledDateAfter(AppointmentStatus status, LocalDate date);

    long countByStatus(AppointmentStatus status);

    List<Appointment> findByDoctorAndScheduledDate(Doctor doctor, LocalDate date);
}