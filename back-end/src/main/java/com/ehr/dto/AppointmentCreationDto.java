package com.ehr.dto;

import com.ehr.models.Appointment;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentCreationDto {
    @NotNull(message = "Appointment Slot ID is required")
    private Long slotId;

    private String notes; // Optional notes from patient
    
    // The type is now set by the service depending on the endpoint called
}
