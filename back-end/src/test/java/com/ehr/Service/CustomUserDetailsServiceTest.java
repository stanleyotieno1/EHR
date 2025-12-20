package com.ehr.Service;

import com.ehr.models.User;
import com.ehr.models.Staff;
import com.ehr.repository.UserRepository;
import com.ehr.repository.StaffRepository;
import com.ehr.service.CustomUserDetailsService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void shouldLoadPatientUserByEmail() {
        // Arrange
        String email = "patient@example.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hashed-password");
        user.setRole("PATIENT");

        when(userRepository.findByEmailOrPhoneNumber(email)).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        // Assert
        assertNotNull(userDetails);
        assertEquals(email, userDetails.getUsername());
        assertEquals("hashed-password", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT")));
        
        verify(userRepository).findByEmailOrPhoneNumber(email);
        verifyNoInteractions(staffRepository);
    }

    @Test
    void shouldLoadStaffByWorkId() {
        // Arrange
        String workId = "STAFF123";
        Staff staff = new Staff();
        staff.setWorkId(workId);
        staff.setPassword("staff-pass");
        staff.setRole(Staff.Role.ADMIN);

        when(userRepository.findByEmailOrPhoneNumber(workId)).thenReturn(Optional.empty());
        when(staffRepository.findByWorkId(workId)).thenReturn(Optional.of(staff));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(workId);

        // Assert
        assertNotNull(userDetails);
        assertEquals(workId, userDetails.getUsername());
        assertEquals("staff-pass", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        
        verify(userRepository).findByEmailOrPhoneNumber(workId);
        verify(staffRepository).findByWorkId(workId);
    }

    @Test
    void shouldThrowExceptionIfUserNotFound() {
        // Arrange
        String identifier = "unknown";
        when(userRepository.findByEmailOrPhoneNumber(identifier)).thenReturn(Optional.empty());
        when(staffRepository.findByWorkId(identifier)).thenReturn(Optional.empty());

        // Act + Assert
        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(identifier)
        );

        assertTrue(ex.getMessage().contains(identifier));

        verify(userRepository).findByEmailOrPhoneNumber(identifier);
        verify(staffRepository).findByWorkId(identifier);
    }

    @Test
    void shouldReturnCorrectUserType() {
        String userIdentifier = "user@example.com";
        String staffIdentifier = "STAFF123";

        when(userRepository.findByEmailOrPhoneNumber(userIdentifier)).thenReturn(Optional.of(new User()));
        when(staffRepository.findByWorkId(staffIdentifier)).thenReturn(Optional.of(new Staff()));

        assertEquals("USER", customUserDetailsService.getUserType(userIdentifier));
        assertEquals("STAFF", customUserDetailsService.getUserType(staffIdentifier));
        assertEquals("UNKNOWN", customUserDetailsService.getUserType("unknown"));
    }
}
