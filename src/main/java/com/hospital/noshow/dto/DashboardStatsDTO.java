package com.hospital.noshow.dto;

import lombok.Data;

import java.util.List;

@Data
public class DashboardStatsDTO {
    private long totalAppointments;
    private long todayAppointmentCount;
    private long highRiskCount;
    private long totalPatients;

    // Parallel lists, not a Map — keeps the Thymeleaf #strings.listJoin binding
    // for Chart.js's data-* attributes trivial (see admin/dashboard.html).
    private List<String> statusLabels;
    private List<Long> statusValues;
    private List<String> riskLabels;
    private List<Long> riskValues;
}