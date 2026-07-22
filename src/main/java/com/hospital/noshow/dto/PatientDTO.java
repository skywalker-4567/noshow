package com.hospital.noshow.dto;

import com.hospital.noshow.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PatientDTO {

    @NotNull(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age cannot be negative")
    private Integer age;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @Size(max = 15)
    private String phone;

    @Email(message = "Must be a valid email address")
    @Size(max = 100)
    private String email;

    private boolean scholarship;
    private boolean hypertension;
    private boolean diabetes;
    private boolean alcoholism;
    private boolean smsReceived;
}