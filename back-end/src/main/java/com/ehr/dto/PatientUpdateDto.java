package com.ehr.dto;

import com.ehr.models.Patient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class PatientUpdateDto {

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Patient.Gender gender;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    private Patient.BloodGroup bloodGroup;

    private Patient.Genotype genotype;

    private Patient.MaritalStatus maritalStatus;

    @Size(max = 100, message = "Occupation must not exceed 100 characters")
    private String occupation;
}
