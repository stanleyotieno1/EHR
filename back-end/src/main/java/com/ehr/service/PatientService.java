package com.ehr.service;

import com.ehr.dto.PatientResponseDto;
import com.ehr.dto.PatientUpdateDto;
import com.ehr.models.Patient;
import com.ehr.models.User;
import com.ehr.repository.PatientRepository;
import com.ehr.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public PatientResponseDto updatePatient(Long patientId, PatientUpdateDto dto) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

        patient.setDateOfBirth(dto.getDateOfBirth());
        patient.setGender(dto.getGender());
        patient.setAddress(dto.getAddress());
        patient.setBloodGroup(dto.getBloodGroup());
        patient.setGenotype(dto.getGenotype());
        patient.setMaritalStatus(dto.getMaritalStatus());
        patient.setOccupation(dto.getOccupation());

        Patient updatedPatient = patientRepository.save(patient);
        return convertToDto(updatedPatient);
    }

    public Optional<PatientResponseDto> getPatientByUserId(String userId) {
        Optional<User> userOptional = userRepository.findByIdWithPatient(userId);
        if (userOptional.isEmpty()) {
            return Optional.empty();
        }
        User user = userOptional.get();
        Patient patient = user.getPatient();
        if (patient == null) {
            return Optional.empty();
        }
        return Optional.of(convertToDto(patient));
    }
    
    public Optional<PatientResponseDto> getPatientById(Long patientId) {
        return patientRepository.findById(patientId).map(this::convertToDto);
    }

    public List<PatientResponseDto> searchPatients(String searchTerm) {
        return patientRepository.searchPatients(searchTerm).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Page<PatientResponseDto> searchPatientsPaginated(String searchTerm, Pageable pageable) {
        Page<Patient> patients = patientRepository.searchPatientsPaginated(searchTerm, pageable);
        return patients.map(this::convertToDto);
    }

    private PatientResponseDto convertToDto(Patient patient) {
        PatientResponseDto dto = new PatientResponseDto();
        dto.setId(patient.getId());
        User user = patient.getUser();
        if (user != null) {
            dto.setUserId(user.getId());
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setEmail(user.getEmail());
            dto.setPhoneNumber(user.getPhoneNumber());
        }
        dto.setDateOfBirth(patient.getDateOfBirth());
        dto.setGender(patient.getGender());
        dto.setAddress(patient.getAddress());
        dto.setBloodGroup(patient.getBloodGroup());
        dto.setGenotype(patient.getGenotype());
        dto.setMaritalStatus(patient.getMaritalStatus());
        dto.setOccupation(patient.getOccupation());
        dto.setCreatedAt(patient.getCreatedAt());
        dto.setUpdatedAt(patient.getUpdatedAt());
        return dto;
    }
}
