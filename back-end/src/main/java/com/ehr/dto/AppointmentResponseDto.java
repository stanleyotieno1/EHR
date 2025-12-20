package com.ehr.dto;

import com.ehr.models.Appointment;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponseDto {
    private Long id;
    private Long patientId;
    private String patientFullName;
    private Long doctorId;
    private String doctorFullName;
    private Long slotId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Appointment.AppointmentStatus status;
    private Appointment.AppointmentType type;
    private String notes;
    private String doctorNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AppointmentResponseDto(Appointment appointment) {
        this.id = appointment.getId();
        // Defensive checks for patient and doctor to avoid NullPointerException if not eager loaded
        if (appointment.getPatient() != null && appointment.getPatient().getUser() != null) {
            this.patientId = appointment.getPatient().getId();
            this.patientFullName = appointment.getPatient().getUser().getFullName();
        }
        if (appointment.getSlot() != null && appointment.getSlot().getDoctor() != null) {
            this.slotId = appointment.getSlot().getId();
            this.doctorId = appointment.getSlot().getDoctor().getId();
            this.doctorFullName = appointment.getSlot().getDoctor().getFullName();
            this.startTime = appointment.getSlot().getStartTime();
            this.endTime = appointment.getSlot().getEndTime();
        }
        this.status = appointment.getStatus();
        this.type = appointment.getType();
        this.notes = appointment.getNotes();
        this.doctorNotes = appointment.getDoctorNotes();
        this.createdAt = appointment.getCreatedAt();
        this.updatedAt = appointment.getUpdatedAt();
    }
}
