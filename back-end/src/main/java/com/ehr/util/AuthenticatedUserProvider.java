package com.ehr.util;

import com.ehr.models.Staff;
import com.ehr.models.User;
import com.ehr.repository.StaffRepository;
import com.ehr.repository.UserRepository;
import com.ehr.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthenticatedUserProvider {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    public Optional<User> getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();

        if ("USER".equals(userDetailsService.getUserType(username))) {
            return userRepository.findByEmailOrPhoneNumber(username);
        }
        return Optional.empty();
    }

    public Optional<Staff> getAuthenticatedStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();

        if ("STAFF".equals(userDetailsService.getUserType(username))) {
            return staffRepository.findByWorkId(username);
        }
        return Optional.empty();
    }
}
