package com.hospital.noshow.service;

import com.hospital.noshow.entity.Doctor;
import com.hospital.noshow.exception.ResourceNotFoundException;
import com.hospital.noshow.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;

    @Override
    public List<Doctor> findAll() {
        return doctorRepository.findAll();
    }

    @Override
    public Doctor findByUsername(String username) {
        return doctorRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No Doctor record linked to user: " + username));
    }
}