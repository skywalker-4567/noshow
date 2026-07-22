package com.hospital.noshow.service;

import com.hospital.noshow.dto.PatientDTO;
import com.hospital.noshow.entity.Patient;

import java.util.List;

public interface PatientService {

    List<Patient> findAll();

    Patient findById(Long id);

    List<Patient> search(String name);

    Patient create(PatientDTO dto);

    Patient update(Long id, PatientDTO dto);

    PatientDTO toDto(Patient patient);

    PatientDTO findDtoById(Long id);
}