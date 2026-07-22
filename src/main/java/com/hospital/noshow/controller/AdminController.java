package com.hospital.noshow.controller;

import com.hospital.noshow.dto.PatientDTO;
import com.hospital.noshow.entity.Patient;
import com.hospital.noshow.enums.RiskLevel;
import com.hospital.noshow.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import com.hospital.noshow.entity.Appointment;
import com.hospital.noshow.entity.RiskScore;
import com.hospital.noshow.enums.AppointmentStatus;
import com.hospital.noshow.repository.RiskScoreRepository;
import com.hospital.noshow.service.AppointmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.hospital.noshow.dto.DashboardStatsDTO;
import com.hospital.noshow.repository.AppointmentRepository;
import com.hospital.noshow.repository.PatientRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.hospital.noshow.entity.AuditLog;
import com.hospital.noshow.repository.AuditLogRepository;
import org.springframework.data.domain.Sort;
import java.time.LocalDateTime;
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final RiskScoreRepository riskScoreRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final AuditLogRepository auditLogRepository;

    @GetMapping("/patients")
    public String listPatients(@RequestParam(required = false) String search, Model model) {
        var patients = (search != null && !search.isBlank())
                ? patientService.search(search)
                : patientService.findAll();
        model.addAttribute("patients", patients);
        model.addAttribute("search", search);
        return "admin/patients";
    }

    @PostMapping("/patients/add")
    public String addPatient(@Valid @ModelAttribute("patientDTO") PatientDTO dto,
                             BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("patients", patientService.findAll());
            model.addAttribute("formMode", "add");
            return "admin/patients";
        }
        patientService.create(dto);
        return "redirect:/admin/patients?added=true";
    }

    @GetMapping("/patients/{id}/edit")
    public String editPatientForm(@PathVariable Long id, Model model) {
        model.addAttribute("patientDTO", patientService.findDtoById(id));
        model.addAttribute("editId", id);
        model.addAttribute("patients", patientService.findAll());
        model.addAttribute("formMode", "edit");
        return "admin/patients";
    }

    @PostMapping("/patients/{id}/edit")
    public String editPatient(@PathVariable Long id,
                              @Valid @ModelAttribute("patientDTO") PatientDTO dto,
                              BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("patients", patientService.findAll());
            model.addAttribute("editId", id);
            model.addAttribute("formMode", "edit");
            return "admin/patients";
        }
        patientService.update(id, dto);
        return "redirect:/admin/patients?updated=true";
    }
    @GetMapping("/appointments")
    public String appointments(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "SCHEDULED") String status,
                               Model model) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by("scheduledDate").descending());
        Page<Appointment> appointments = appointmentService
                .findByStatus(AppointmentStatus.valueOf(status), pageable);

        // Auxiliary per-row lookup for risk badges — same direct-repository
        // precedent as ReceptionistController's dashboard HIGH-risk list.
        Map<Long, RiskScore> riskByAppointmentId = appointments.getContent().stream()
                .map(a -> riskScoreRepository.findByAppointmentId(a.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(rs -> rs.getAppointment().getId(), rs -> rs));

        model.addAttribute("appointments", appointments);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", appointments.getTotalPages());
        model.addAttribute("currentStatus", status);
        model.addAttribute("riskByAppointmentId", riskByAppointmentId);
        return "admin/appointments";
    }
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", buildDashboardStats());
        return "admin/dashboard";
    }

    private DashboardStatsDTO buildDashboardStats() {
        DashboardStatsDTO dto = new DashboardStatsDTO();
        dto.setTotalAppointments(appointmentRepository.count());
        dto.setTodayAppointmentCount(appointmentRepository.findByScheduledDate(LocalDate.now()).size());
        dto.setHighRiskCount(riskScoreRepository.findByRiskLevel(RiskLevel.HIGH).size());
        dto.setTotalPatients(patientRepository.count());

        List<String> statusLabels = new ArrayList<>();
        List<Long> statusValues = new ArrayList<>();
        for (AppointmentStatus s : AppointmentStatus.values()) {
            statusLabels.add(s.name());
            statusValues.add(appointmentRepository.countByStatus(s));
        }
        dto.setStatusLabels(statusLabels);
        dto.setStatusValues(statusValues);

        List<String> riskLabels = new ArrayList<>();
        List<Long> riskValues = new ArrayList<>();
        for (RiskLevel r : RiskLevel.values()) {
            riskLabels.add(r.name());
            riskValues.add(riskScoreRepository.countByRiskLevel(r));
        }
        dto.setRiskLabels(riskLabels);
        dto.setRiskValues(riskValues);

        return dto;
    }
    @GetMapping("/audit-log")
    public String auditLog(Model model) {
        List<AuditLog> logs = auditLogRepository.findAll(
                Sort.by(Sort.Direction.DESC, "changedAt"));
        model.addAttribute("logs", logs);
        return "admin/audit-log";
    }

    @GetMapping("/risk-report")
    public String riskReport(Model model) {
        LocalDate weekAgo = LocalDate.now().minusDays(7);

        long predictedHigh = riskScoreRepository
                .countByRiskLevelAndScoredAtAfter(RiskLevel.HIGH, weekAgo.atStartOfDay());
        long actualNoShow = appointmentRepository
                .countByStatusAndScheduledDateAfter(AppointmentStatus.NO_SHOW, weekAgo);
        long truePositives = riskScoreRepository
                .countByRiskLevelAndAppointment_Status(RiskLevel.HIGH, AppointmentStatus.NO_SHOW);

        // Precision guard: avoid divide-by-zero when no HIGH predictions exist yet
        double precision = predictedHigh > 0
                ? (double) truePositives / predictedHigh
                : 0.0;

        model.addAttribute("predictedHigh", predictedHigh);
        model.addAttribute("actualNoShow", actualNoShow);
        model.addAttribute("truePositives", truePositives);
        model.addAttribute("precision", String.format("%.1f%%", precision * 100));
        return "admin/risk-report";
    }
}