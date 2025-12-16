package com.ehr.controller;

import com.ehr.dto.PatientResponseDto;
import com.ehr.dto.PatientUpdateDto;
import com.ehr.exception.UnauthorizedException;
import com.ehr.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    @Autowired
    private PatientService patientService;

    /**
     * Helper method to check if current user is staff
     */
    private boolean isStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));
    }

    /**
     * Helper method to verify user owns the patient record
     */
    private void verifyPatientOwnership(Long patientId) {
        if (isStaff()) return; // Staff can access all patients

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("Authentication required");
        }

        String username = auth.getName();
        PatientResponseDto patient = patientService.getPatientById(patientId)
                .orElseThrow(() -> new UnauthorizedException("Patient not found"));

        if (!patient.getEmail().equals(username)) {
            throw new UnauthorizedException("You are not authorized to access this patient's data");
        }
    }

    /**
     * Helper method to verify user is accessing their own data by userId
     */
    private void verifyUserOwnership(String userId) {
        if (isStaff()) return; // Staff can access all users

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new UnauthorizedException("Authentication required");
        }

        String username = auth.getName();
        PatientResponseDto patient = patientService.getPatientByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!patient.getEmail().equals(username)) {
            throw new UnauthorizedException("You are not authorized to access this user's data");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientResponseDto> updatePatient(@PathVariable Long id, @Valid @RequestBody PatientUpdateDto dto) {
        verifyPatientOwnership(id);
        PatientResponseDto updatedPatient = patientService.updatePatient(id, dto);
        return ResponseEntity.ok(updatedPatient);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponseDto> getPatientById(@PathVariable Long id) {
        verifyPatientOwnership(id);
        return patientService.getPatientById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PatientResponseDto> getPatientByUserId(@PathVariable String userId) {
        verifyUserOwnership(userId);
        return patientService.getPatientByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<Page<PatientResponseDto>> searchPatients(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PatientResponseDto> patients = patientService.searchPatientsPaginated(searchTerm, pageable);
        return ResponseEntity.ok(patients);
    }
}
