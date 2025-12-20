package com.ehr.service;

import com.ehr.dto.*;
import com.ehr.exception.ResourceNotFoundException;
import com.ehr.exception.UnauthorizedException;
import com.ehr.models.*;
import com.ehr.repository.*;
import com.ehr.util.AuthenticatedUserProvider;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private AppointmentSlotRepository appointmentSlotRepository;
    @Autowired
    private StaffRepository staffRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public AppointmentSlotResponseDto createAppointmentSlot(AppointmentSlotCreationDto dto) {
        Staff doctor = staffRepository.findById(dto.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + dto.getDoctorId()));

        if (doctor.getRole() != Staff.Role.DOCTOR) {
            throw new IllegalArgumentException("Staff with ID " + dto.getDoctorId() + " is not a doctor.");
        }

        Staff currentUser = authenticatedUserProvider.getAuthenticatedStaff()
                .orElseThrow(() -> new UnauthorizedException("Action requires a staff account."));

        if (currentUser.getRole() != Staff.Role.ADMIN && !currentUser.getId().equals(doctor.getId())) {
            throw new UnauthorizedException("Doctors can only create slots for themselves.");
        }

        if (dto.getStartTime().isAfter(dto.getEndTime()) || dto.getStartTime().isEqual(dto.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time.");
        }

        AppointmentSlot slot = new AppointmentSlot();
        slot.setDoctor(doctor);
        slot.setStartTime(dto.getStartTime());
        slot.setEndTime(dto.getEndTime());
        slot.setStatus(AppointmentSlot.SlotStatus.AVAILABLE);

        AppointmentSlot savedSlot = appointmentSlotRepository.save(slot);
        return new AppointmentSlotResponseDto(savedSlot);
    }

    @Transactional
    public AppointmentResponseDto bookAppointment(AppointmentCreationDto dto) {
        // Get the currently authenticated user and their patient profile. This is the source of truth.
        User currentUser = authenticatedUserProvider.getAuthenticatedUser()
                .orElseThrow(() -> new UnauthorizedException("Action requires a patient account."));
        Patient patient = patientRepository.findByUser(currentUser)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user does not have a patient profile."));

        AppointmentSlot slot = appointmentSlotRepository.findById(dto.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment slot not found with ID: " + dto.getSlotId()));

        if (slot.getStatus() != AppointmentSlot.SlotStatus.AVAILABLE) {
            throw new IllegalArgumentException("Appointment slot is not available.");
        }

        slot.setStatus(AppointmentSlot.SlotStatus.BOOKED);
        appointmentSlotRepository.save(slot);

        Appointment appointment = new Appointment();
        appointment.setPatient(patient); // Use the authenticated patient
        appointment.setSlot(slot);
        appointment.setStatus(Appointment.AppointmentStatus.SCHEDULED);
        appointment.setType(Appointment.AppointmentType.ONLINE); // This endpoint is for online bookings
        appointment.setNotes(dto.getNotes());

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return new AppointmentResponseDto(savedAppointment);
    }
    
    @Transactional
    public AppointmentResponseDto bookWalkInAppointment(WalkInPatientCreationDto dto) {
        // This method is only callable by RECEPTIONIST/ADMIN, so no ownership check is needed
        // as they can create appointments for any patient.
        Optional<User> existingUserByEmail = userRepository.findByEmail(dto.getEmail());
        if (existingUserByEmail.isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        Optional<User> existingUserByPhone = userRepository.findByPhoneNumber(dto.getPhoneNumber());
        if (existingUserByPhone.isPresent()) {
            throw new IllegalArgumentException("An account with this phone number already exists.");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPasswordHash(passwordEncoder.encode("defaultPassword123!"));
        user.setRole("USER");
        user.setVerified(true);
        user = userRepository.save(user);

        Patient patient = new Patient();
        patient.setUser(user);
        patient.setDateOfBirth(dto.getDateOfBirth());
        patient.setGender(dto.getGender());
        patient.setAddress(dto.getAddress());
        patient.setBloodGroup(dto.getBloodGroup());
        patient.setGenotype(dto.getGenotype());
        patient.setMaritalStatus(dto.getMaritalStatus());
        patient.setOccupation(dto.getOccupation());
        patient = patientRepository.save(patient);

        // Directly create the appointment entity and populate its fields for walk-in booking.
        // No intermediate DTO is needed here.
        // The patient ownership checks are bypassed as this method is for receptionists.
        AppointmentSlot slot = appointmentSlotRepository.findById(dto.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment slot not found with ID: " + dto.getSlotId()));

        if (slot.getStatus() != AppointmentSlot.SlotStatus.AVAILABLE) {
            throw new IllegalArgumentException("Appointment slot is not available.");
        }
        
        slot.setStatus(AppointmentSlot.SlotStatus.BOOKED);
        appointmentSlotRepository.save(slot);

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setSlot(slot);
        appointment.setStatus(Appointment.AppointmentStatus.SCHEDULED);
        appointment.setType(Appointment.AppointmentType.WALK_IN);
        appointment.setNotes(dto.getNotes());

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return new AppointmentResponseDto(savedAppointment);
    }
    
    @Transactional
    public List<AppointmentSlotResponseDto> getAvailableSlots(LocalDateTime from, LocalDateTime to) {
        List<AppointmentSlot> slots = appointmentSlotRepository.findByStatusAndStartTimeBetweenOrderByStartTimeAsc(
                AppointmentSlot.SlotStatus.AVAILABLE, from, to);
        return slots.stream()
                .map(AppointmentSlotResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<AppointmentSlotResponseDto> getDoctorSlots(Long doctorId, LocalDateTime from, LocalDateTime to) {
        Staff doctor = staffRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));
        
        Staff currentUser = authenticatedUserProvider.getAuthenticatedStaff()
                .orElse(null); // Can be null if patient is logged in

        if (currentUser != null && currentUser.getRole() == Staff.Role.DOCTOR && !currentUser.getId().equals(doctor.getId())) {
             throw new UnauthorizedException("Doctors can only view their own available slots.");
        }

        List<AppointmentSlot> slots = appointmentSlotRepository.findByDoctorAndStartTimeBetweenOrderByStartTimeAsc(
                doctor, from, to);
        return slots.stream()
                .map(AppointmentSlotResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<AppointmentResponseDto> getPatientAppointments(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));
        
        Optional<User> currentUserOpt = authenticatedUserProvider.getAuthenticatedUser();
        if(currentUserOpt.isPresent()) {
            Patient authenticatedPatient = patientRepository.findByUser(currentUserOpt.get())
                    .orElseThrow(() -> new UnauthorizedException("Authenticated user does not have a patient profile."));
            if(!authenticatedPatient.getId().equals(patient.getId())) {
                throw new UnauthorizedException("Patients can only view their own appointments.");
            }
        }

        List<Appointment> appointments = appointmentRepository.findByPatientOrderBySlot_StartTimeDesc(patient);
        return appointments.stream()
                .map(AppointmentResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<AppointmentResponseDto> getDoctorAppointments(Long doctorId) {
        Staff doctor = staffRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        Staff currentUser = authenticatedUserProvider.getAuthenticatedStaff()
                .orElse(null);

        if (currentUser != null && currentUser.getRole() == Staff.Role.DOCTOR && !currentUser.getId().equals(doctor.getId())) {
            throw new UnauthorizedException("Doctors can only view their own appointments.");
        }
        
        List<Appointment> appointments = appointmentRepository.findBySlot_DoctorOrderBySlot_StartTimeDesc(doctor);
        return appointments.stream()
                .map(AppointmentResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentResponseDto updateAppointmentStatus(Long appointmentId, Appointment.AppointmentStatus newStatus) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        Staff currentUser = authenticatedUserProvider.getAuthenticatedStaff()
                .orElse(null);
        
        if (currentUser != null && currentUser.getRole() == Staff.Role.DOCTOR && !currentUser.getId().equals(appointment.getSlot().getDoctor().getId())) {
            throw new UnauthorizedException("Doctors can only update the status of their own appointments.");
        }

        if (newStatus == Appointment.AppointmentStatus.CANCELLED) {
            AppointmentSlot slot = appointment.getSlot();
            if (slot != null && slot.getStartTime().isAfter(LocalDateTime.now())) { // Can only cancel future appointments
                slot.setStatus(AppointmentSlot.SlotStatus.AVAILABLE);
                appointmentSlotRepository.save(slot);
            }
        }
        appointment.setStatus(newStatus);
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        return new AppointmentResponseDto(updatedAppointment);
    }
    
    @Transactional
    public AppointmentResponseDto addDoctorNotesToAppointment(Long appointmentId, String doctorNotes) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));
        
        Staff currentUser = authenticatedUserProvider.getAuthenticatedStaff()
                .orElseThrow(() -> new UnauthorizedException("Action requires a staff account."));

        if (currentUser.getRole() != Staff.Role.ADMIN && !currentUser.getId().equals(appointment.getSlot().getDoctor().getId())) {
            throw new UnauthorizedException("Doctors can only add notes to their own appointments.");
        }

        appointment.setDoctorNotes(doctorNotes);
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        return new AppointmentResponseDto(updatedAppointment);
    }
}
