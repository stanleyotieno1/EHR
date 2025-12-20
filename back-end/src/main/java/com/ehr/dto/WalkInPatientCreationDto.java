package com.ehr.dto;

import com.ehr.models.Patient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalkInPatientCreationDto {

    // User details
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String email;

    @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Invalid phone number format")
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Patient.Gender gender;

    @NotBlank(message = "Address is required")
    private String address;

    private Patient.BloodGroup bloodGroup;

    private Patient.Genotype genotype;

    private Patient.MaritalStatus maritalStatus;

    private String occupation; // Optional

    // Appointment details
    @NotNull(message = "Appointment Slot ID is required")
    private Long slotId;

    private String notes; // Optional notes for the appointment
}
