package com.ehr.controller;

import com.ehr.dto.*;
import com.ehr.models.Appointment;
import com.ehr.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    // Doctor creates available slots
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @PostMapping("/slots")
    public ResponseEntity<AppointmentSlotResponseDto> createAppointmentSlot(@Valid @RequestBody AppointmentSlotCreationDto dto) {
        AppointmentSlotResponseDto createdSlot = appointmentService.createAppointmentSlot(dto);
        return new ResponseEntity<>(createdSlot, HttpStatus.CREATED);
    }

    // Patient books an appointment
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/book")
    public ResponseEntity<AppointmentResponseDto> bookAppointment(@Valid @RequestBody AppointmentCreationDto dto) {
        AppointmentResponseDto bookedAppointment = appointmentService.bookAppointment(dto);
        return new ResponseEntity<>(bookedAppointment, HttpStatus.CREATED);
    }

    // Receptionist books a walk-in appointment (can create new patient)
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    @PostMapping("/walkin")
    public ResponseEntity<AppointmentResponseDto> bookWalkInAppointment(@Valid @RequestBody WalkInPatientCreationDto dto) {
        AppointmentResponseDto bookedAppointment = appointmentService.bookWalkInAppointment(dto);
        return new ResponseEntity<>(bookedAppointment, HttpStatus.CREATED);
    }

    // Get all available slots (publicly accessible or for all authenticated users)
    @GetMapping("/slots/available")
    public ResponseEntity<List<AppointmentSlotResponseDto>> getAvailableSlots(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        // Default to a reasonable range if not provided
        if (from == null) from = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        if (to == null) to = from.plusMonths(1).withHour(23).withMinute(59).withSecond(59);

        List<AppointmentSlotResponseDto> slots = appointmentService.getAvailableSlots(from, to);
        return ResponseEntity.ok(slots);
    }

    // Get slots for a specific doctor
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'USER')")
    @GetMapping("/doctors/{doctorId}/slots")
    public ResponseEntity<List<AppointmentSlotResponseDto>> getDoctorSlots(
            @PathVariable Long doctorId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (from == null) from = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        if (to == null) to = from.plusMonths(1).withHour(23).withMinute(59).withSecond(59);

        List<AppointmentSlotResponseDto> slots = appointmentService.getDoctorSlots(doctorId, from, to);
        return ResponseEntity.ok(slots);
    }

    // Get appointments for a patient
    @PreAuthorize("hasAnyRole('USER', 'RECEPTIONIST', 'ADMIN')")
    @GetMapping("/patients/{patientId}")
    public ResponseEntity<List<AppointmentResponseDto>> getPatientAppointments(
            @PathVariable Long patientId) {
        List<AppointmentResponseDto> appointments = appointmentService.getPatientAppointments(patientId);
        return ResponseEntity.ok(appointments);
    }

    // Get appointments for a doctor
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'ADMIN')")
    @GetMapping("/doctors/{doctorId}/appointments")
    public ResponseEntity<List<AppointmentResponseDto>> getDoctorAppointments(
            @PathVariable Long doctorId) {
        List<AppointmentResponseDto> appointments = appointmentService.getDoctorAppointments(doctorId);
        return ResponseEntity.ok(appointments);
    }

    // Update appointment status (e.g., cancel, complete)
    @PreAuthorize("hasAnyRole('DOCTOR', 'RECEPTIONIST', 'ADMIN')")
    @PutMapping("/{appointmentId}/status")
    public ResponseEntity<AppointmentResponseDto> updateAppointmentStatus(
            @PathVariable Long appointmentId,
            @RequestParam Appointment.AppointmentStatus newStatus) {
        AppointmentResponseDto updatedAppointment = appointmentService.updateAppointmentStatus(appointmentId, newStatus);
        return ResponseEntity.ok(updatedAppointment);
    }

    // Add doctor's notes after appointment
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @PutMapping("/{appointmentId}/doctor-notes")
    public ResponseEntity<AppointmentResponseDto> addDoctorNotesToAppointment(
            @PathVariable Long appointmentId,
            @RequestBody String doctorNotes) {
        AppointmentResponseDto updatedAppointment = appointmentService.addDoctorNotesToAppointment(appointmentId, doctorNotes);
        return ResponseEntity.ok(updatedAppointment);
    }
}
