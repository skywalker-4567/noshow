package com.hospital.noshow.controller;

import com.hospital.noshow.dto.AppointmentDTO;
import com.hospital.noshow.enums.RiskLevel;
import com.hospital.noshow.repository.RiskScoreRepository;
import com.hospital.noshow.service.AppointmentService;
import com.hospital.noshow.service.DoctorService;
import com.hospital.noshow.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@RequestMapping("/receptionist")
public class ReceptionistController {

    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final RiskScoreRepository riskScoreRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("todayAppointments", appointmentService.findByDate(today));
        model.addAttribute("highRiskToday",
                riskScoreRepository.findByRiskLevelAndAppointment_ScheduledDate(RiskLevel.HIGH, today));
        return "receptionist/dashboard";
    }

    @GetMapping("/appointments")
    public String listAppointments(Model model) {
        model.addAttribute("appointments", appointmentService.findAll());
        return "receptionist/appointment-list";
    }

    @GetMapping("/appointments/new")
    public String newAppointmentForm(Model model) {
        model.addAttribute("appointmentDTO", new AppointmentDTO());
        model.addAttribute("patients", patientService.findAll());
        model.addAttribute("doctors", doctorService.findAll());
        return "receptionist/appointment-form";
    }

    @PostMapping("/appointments/new")
    public String bookAppointment(@Valid @ModelAttribute("appointmentDTO") AppointmentDTO dto,
                                  BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("patients", patientService.findAll());
            model.addAttribute("doctors", doctorService.findAll());
            return "receptionist/appointment-form";
        }
        appointmentService.book(dto);
        return "redirect:/receptionist/appointments?booked=true";
    }

    @PostMapping("/appointments/{id}/confirm")
    public String confirm(@PathVariable Long id) {
        appointmentService.confirm(id);
        return "redirect:/receptionist/appointments?confirmed=true";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancel(@PathVariable Long id) {
        appointmentService.cancel(id);
        return "redirect:/receptionist/appointments?cancelled=true";
    }

    @PostMapping("/appointments/{id}/no-show")
    public String noShow(@PathVariable Long id) {
        appointmentService.markNoShow(id);
        return "redirect:/receptionist/appointments?noshow=true";
    }

    @PostMapping("/appointments/{id}/complete")
    public String complete(@PathVariable Long id) {
        appointmentService.complete(id);
        return "redirect:/receptionist/appointments?completed=true";
    }
}