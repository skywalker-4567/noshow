package com.hospital.noshow.service;

import com.hospital.noshow.dto.AppointmentDTO;
import com.hospital.noshow.entity.Appointment;
import com.hospital.noshow.entity.Doctor;
import com.hospital.noshow.entity.Patient;
import com.hospital.noshow.enums.AppointmentStatus;
import com.hospital.noshow.exception.ResourceNotFoundException;
import com.hospital.noshow.repository.AppointmentRepository;
import com.hospital.noshow.repository.DoctorRepository;
import com.hospital.noshow.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AuditService auditService;
    private final EmailService emailService;

    @Override
    public List<Appointment> findAll() {
        return appointmentRepository.findAll();
    }

    @Override
    public Appointment findById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
    }

    @Override
    public List<Appointment> findByDate(LocalDate date) {
        return appointmentRepository.findByScheduledDate(date);
    }

    @Override
    public Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable) {
        return appointmentRepository.findByStatus(status, pageable);
    }

    @Override
    public List<Appointment> findByDoctorAndDate(Doctor doctor, LocalDate date) {
        return appointmentRepository.findByDoctorAndScheduledDate(doctor, date);
    }

    @Transactional
    @Override
    public void book(AppointmentDTO dto) {
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient not found: " + dto.getPatientId()));
        Doctor doctor = doctorRepository.findById(dto.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found: " + dto.getDoctorId()));

        String actor = currentUsername();

        Appointment appt = new Appointment();
        appt.setPatient(patient);
        appt.setDoctor(doctor);
        appt.setScheduledDate(dto.getScheduledDate());
        appt.setScheduledTime(dto.getScheduledTime());
        appt.setNotes(dto.getNotes());
        appt.setStatus(AppointmentStatus.SCHEDULED);
        appt.setCreatedBy(actor);

        appointmentRepository.save(appt);

        // §16 Week 2 checkpoint: booking itself must produce an audit row,
        // not just the subsequent status transition.
        auditService.writeLog("APPOINTMENT", appt.getId(), null, "SCHEDULED", actor);

        emailService.sendAppointmentConfirmation(appt);  // @Async — fires after commit
    }

    @Transactional   // on every method that touches more than one table
    @Override
    public void confirm(Long appointmentId) {
        Appointment appt = findById(appointmentId);
        String oldStatus = appt.getStatus().name();
        appt.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(appt);
        auditService.writeLog("APPOINTMENT", appointmentId, oldStatus, "CONFIRMED", currentUsername());
        emailService.sendStatusUpdateToPatient(appt);
    }

    @Transactional
    @Override
    public void markNoShow(Long appointmentId) {
        Appointment appt = findById(appointmentId);
        String oldStatus = appt.getStatus().name();
        appt.setStatus(AppointmentStatus.NO_SHOW);
        appointmentRepository.save(appt);
        auditService.writeLog("APPOINTMENT", appointmentId, oldStatus, "NO_SHOW", currentUsername());
        emailService.sendNoShowNotification(appt);
    }

    @Transactional
    @Override
    public void cancel(Long appointmentId) {
        Appointment appt = findById(appointmentId);
        String oldStatus = appt.getStatus().name();
        appt.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appt);
        auditService.writeLog("APPOINTMENT", appointmentId, oldStatus, "CANCELLED", currentUsername());
        emailService.sendStatusUpdateToPatient(appt);
    }

    @Transactional
    @Override
    public void complete(Long appointmentId) {
        Appointment appt = findById(appointmentId);
        String oldStatus = appt.getStatus().name();
        appt.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appt);
        auditService.writeLog("APPOINTMENT", appointmentId, oldStatus, "COMPLETED", currentUsername());
        emailService.sendStatusUpdateToPatient(appt);
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}