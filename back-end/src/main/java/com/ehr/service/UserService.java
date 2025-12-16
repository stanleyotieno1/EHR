package com.ehr.service;

import com.ehr.dto.UserRegistrationDto;
import com.ehr.models.User;
import com.ehr.models.Patient;
import com.ehr.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(@Valid UserRegistrationDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Check if phone number exists (if provided)
        if (dto.getPhoneNumber() != null &&
                !dto.getPhoneNumber().isBlank() &&
                userRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());

        Patient patient = new Patient();
        patient.setUser(user);
        user.setPatient(patient);

        return userRepository.save(user);
    }

    public Optional<User> findByEmailOrPhone(@NotBlank(message = "Email or phone number is required") String identifier) {
        return this.userRepository.findByEmailOrPhoneNumber(identifier);
    }
}
