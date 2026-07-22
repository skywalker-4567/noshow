package com.hospital.noshow.service;

import com.hospital.noshow.dto.AppointmentDTO;
import com.hospital.noshow.entity.Appointment;
import com.hospital.noshow.entity.Doctor;
import com.hospital.noshow.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    List<Appointment> findAll();

    Appointment findById(Long id);

    List<Appointment> findByDate(LocalDate date);

    Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable);

    void book(AppointmentDTO dto);

    void confirm(Long appointmentId);

    void markNoShow(Long appointmentId);

    void cancel(Long appointmentId);

    void complete(Long appointmentId);

    List<Appointment> findByDoctorAndDate(Doctor doctor, LocalDate date);
}