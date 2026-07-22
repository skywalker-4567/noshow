package com.hospital.noshow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RiskPredictionResponse {

    @JsonProperty("risk_score")
    private double riskScore;

    @JsonProperty("risk_level")
    private String riskLevel;

    @JsonProperty("model_version")
    private String modelVersion;
}