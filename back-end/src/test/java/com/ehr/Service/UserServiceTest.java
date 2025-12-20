package com.ehr.Service;

import com.ehr.dto.UserRegistrationDto;
import com.ehr.models.User;
import com.ehr.repository.UserRepository;
import com.ehr.service.UserService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldRegisterUserSuccessfully() {
        // Arrange
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail("test@example.com");
        dto.setPhoneNumber("0712345678");
        dto.setPassword("password123");
        dto.setFirstName("John");
        dto.setLastName("Doe");

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(dto.getPhoneNumber())).thenReturn(false);
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User savedUser = userService.registerUser(dto);

        // Assert
        assertNotNull(savedUser);
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("hashed-password", savedUser.getPasswordHash());
        assertNotNull(savedUser.getPatient());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldFailWhenEmailAlreadyExists() {
        // Arrange
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail("existing@example.com");
        dto.setPassword("password");

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(true);

        // Act + Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(dto)
        );

        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenPhoneNumberAlreadyExists() {
        // Arrange
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail("new@example.com");
        dto.setPhoneNumber("0712345678");
        dto.setPassword("password");

        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(dto.getPhoneNumber())).thenReturn(true);

        // Act + Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.registerUser(dto)
        );

        verify(userRepository, never()).save(any());
    }
}
