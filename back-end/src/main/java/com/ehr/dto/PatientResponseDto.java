package com.ehr.dto;

import com.ehr.models.Patient;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PatientResponseDto {
    private Long id;
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private Patient.Gender gender;
    private String address;
    private Patient.BloodGroup bloodGroup;
    private Patient.Genotype genotype;
    private Patient.MaritalStatus maritalStatus;
    private String occupation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
