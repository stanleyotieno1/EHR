package com.ehr.dto;

import com.ehr.models.AppointmentSlot;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSlotResponseDto {
    private Long id;
    private Long doctorId;
    private String doctorFullName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AppointmentSlot.SlotStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AppointmentSlotResponseDto(AppointmentSlot slot) {
        this.id = slot.getId();
        // Defensive check for doctor to avoid NullPointerException if not eager loaded or set
        if (slot.getDoctor() != null) {
            this.doctorId = slot.getDoctor().getId();
            this.doctorFullName = slot.getDoctor().getFullName();
        }
        this.startTime = slot.getStartTime();
        this.endTime = slot.getEndTime();
        this.status = slot.getStatus();
        this.createdAt = slot.getCreatedAt();
        this.updatedAt = slot.getUpdatedAt();
    }
}
