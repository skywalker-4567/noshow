package com.hospital.noshow.dto;

import lombok.Data;

@Data
public class RiskPredictionRequest {
    private int age;
    private String gender;
    private int scholarship;
    private int hypertension;
    private int diabetes;
    private int alcoholism;
    private int smsReceived;
    private int daysWaiting;
}