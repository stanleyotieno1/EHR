package com.ehr.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(name = "appointment_slots")
public class AppointmentSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Staff doctor;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlotStatus status = SlotStatus.AVAILABLE;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum SlotStatus {
        AVAILABLE, BOOKED, CANCELLED
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        verifyDoctorRole();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        verifyDoctorRole();
    }

    private void verifyDoctorRole() {
        if (this.doctor != null && this.doctor.getRole() != Staff.Role.DOCTOR) {
            throw new IllegalStateException("Only staff with DOCTOR role can have appointment slots.");
        }
    }
}
