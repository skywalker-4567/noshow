package com.hospital.noshow.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AppointmentDTO {

    @NotNull(message = "Patient is required")
    private Long patientId;

    @NotNull(message = "Doctor is required")
    private Long doctorId;

    @NotNull(message = "Date is required")
    @FutureOrPresent(message = "Appointment date cannot be in the past")
    private LocalDate scheduledDate;

    @NotNull(message = "Time is required")
    private LocalTime scheduledTime;

    private String notes;
}