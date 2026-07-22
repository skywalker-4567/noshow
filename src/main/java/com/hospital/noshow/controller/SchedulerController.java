package com.hospital.noshow.controller;

import com.hospital.noshow.scheduler.HospitalScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/scheduler")
public class SchedulerController {

    private final HospitalScheduler hospitalScheduler;

    @PostMapping("/run-scoring")
    public String runScoring() {
        hospitalScheduler.runNightlyBatchScoring();
        return "redirect:/admin/dashboard?scoringRun=true";
    }

    @PostMapping("/run-morning-alerts")
    public String runMorningAlerts() {
        hospitalScheduler.sendMorningRiskAlerts();
        return "redirect:/admin/dashboard?alertsRun=true";
    }

    @PostMapping("/run-weekly-report")
    public String runWeeklyReport() {
        hospitalScheduler.generateWeeklyAccuracyReport();
        return "redirect:/admin/dashboard?reportRun=true";
    }

}