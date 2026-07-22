package com.hospital.noshow.service;

import com.hospital.noshow.entity.Doctor;

import java.util.List;

public interface DoctorService {
    List<Doctor> findAll();

    Doctor findByUsername(String username);
}