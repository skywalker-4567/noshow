package com.hospital.noshow.controller;

import com.hospital.noshow.entity.Appointment;
import com.hospital.noshow.entity.Doctor;
import com.hospital.noshow.entity.RiskScore;
import com.hospital.noshow.repository.RiskScoreRepository;
import com.hospital.noshow.service.AppointmentService;
import com.hospital.noshow.service.DoctorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/doctor")
public class DoctorController {

    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final RiskScoreRepository riskScoreRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Doctor doctor = doctorService.findByUsername(username);

        List<Appointment> todaySchedule = appointmentService.findByDoctorAndDate(doctor, LocalDate.now());

        // Same auxiliary per-row risk-badge lookup pattern as admin/appointments.html (item 16)
        Map<Long, RiskScore> riskByAppointmentId = new HashMap<>();
        for (Appointment appt : todaySchedule) {
            Optional<RiskScore> rs = riskScoreRepository.findByAppointmentId(appt.getId());
            rs.ifPresent(r -> riskByAppointmentId.put(appt.getId(), r));
        }

        model.addAttribute("doctor", doctor);
        model.addAttribute("todaySchedule", todaySchedule);
        model.addAttribute("riskByAppointmentId", riskByAppointmentId);
        return "doctor/dashboard";
    }
}