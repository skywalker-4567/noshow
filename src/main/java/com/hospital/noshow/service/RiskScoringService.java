package com.hospital.noshow.service;

import com.hospital.noshow.entity.Appointment;

public interface RiskScoringService {
    void scoreAndPersist(Appointment appt);
}