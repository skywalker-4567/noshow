package com.hospital.noshow.service;

import com.hospital.noshow.dto.PatientDTO;
import com.hospital.noshow.entity.Patient;
import com.hospital.noshow.exception.ResourceNotFoundException;
import com.hospital.noshow.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    @Override
    public List<Patient> findAll() {
        return patientRepository.findAll();
    }

    @Override
    public Patient findById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id));
    }

    @Override
    public List<Patient> search(String name) {
        return patientRepository.findByNameContainingIgnoreCase(name);
    }

    @Override
    public Patient create(PatientDTO dto) {
        Patient patient = new Patient();
        applyDtoToEntity(dto, patient);
        return patientRepository.save(patient);
    }

    @Override
    public Patient update(Long id, PatientDTO dto) {
        Patient patient = findById(id);
        applyDtoToEntity(dto, patient);
        return patientRepository.save(patient);
    }

    @Override
    public PatientDTO toDto(Patient patient) {
        PatientDTO dto = new PatientDTO();
        dto.setName(patient.getName());
        dto.setAge(patient.getAge());
        dto.setGender(patient.getGender());
        dto.setPhone(patient.getPhone());
        dto.setEmail(patient.getEmail());
        dto.setScholarship(patient.isScholarship());
        dto.setHypertension(patient.isHypertension());
        dto.setDiabetes(patient.isDiabetes());
        dto.setAlcoholism(patient.isAlcoholism());
        dto.setSmsReceived(patient.isSmsReceived());
        return dto;
    }

    @Override
    public PatientDTO findDtoById(Long id) {
        return toDto(findById(id));
    }

    private void applyDtoToEntity(PatientDTO dto, Patient patient) {
        patient.setName(dto.getName());
        patient.setAge(dto.getAge());
        patient.setGender(dto.getGender());
        patient.setPhone(dto.getPhone());
        patient.setEmail(dto.getEmail());
        patient.setScholarship(dto.isScholarship());
        patient.setHypertension(dto.isHypertension());
        patient.setDiabetes(dto.isDiabetes());
        patient.setAlcoholism(dto.isAlcoholism());
        patient.setSmsReceived(dto.isSmsReceived());
    }
}