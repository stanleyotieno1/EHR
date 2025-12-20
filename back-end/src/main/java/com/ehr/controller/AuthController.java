package com.ehr.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.ehr.dto.*;
import com.ehr.models.User;
import com.ehr.models.Staff;
import com.ehr.service.UserService;
import com.ehr.service.StaffService;
import com.ehr.service.CustomUserDetailsService;
import com.ehr.service.JwtTokenProvider;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private StaffService staffService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @GetMapping("")
    public String hello(){
        return "Hello from spring boot";
    }

    @PostMapping("/users/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDto dto) {
        try {
            User user = userService.registerUser(dto);
            return ResponseEntity.ok(new AuthResponse(
                    "User registered successfully",
                    "USER"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    new AuthResponse(e.getMessage(), "ERROR")
            );
        }
    }

    @PostMapping("/users/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody UserLoginRequestDto req,
                                       HttpServletResponse response) {
        try {
            // Determine the actual username based on identifier
            String username = userService.findByEmailOrPhone(req.getIdentifier())
                    .map(User::getEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(username, req.getPassword());

            Authentication auth = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(auth);

            User user = userService.findByEmailOrPhone(req.getIdentifier()).get();

            // Generate JWT token
            String token = jwtTokenProvider.generateToken(username, "USER");

            // Set JWT in HttpOnly cookie
            setJwtCookie(response, token);

            return ResponseEntity.ok(new AuthResponse(
                    "Login successful",
                    "USER",
                    user.getId(),
                    user.getFullName(),
                    user.getRole()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(
                    new AuthResponse("Login failed: " + e.getMessage(), "ERROR")
            );
        }
    }

    @PostMapping("/staff/create")
    public ResponseEntity<?> createStaff(@RequestBody StaffCreateDto dto) {
        Staff staff = staffService.createStaff(dto);
        return ResponseEntity.ok(new AuthResponse(
                "Staff created successfully",
                "STAFF"
        ));
    }

    @PostMapping("/staff/login")
    public ResponseEntity<?> loginStaff(@RequestBody StaffLoginRequest req,
                                        HttpServletResponse response) {
        try {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(req.getWorkId(), req.getPassword());

            Authentication auth = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(auth);

            Optional<Staff> staff = staffService.findByWorkId(req.getWorkId());

            if (staff.isPresent()) {
                Staff foundStaff = staff.get();

                // Generate JWT token
                String token = jwtTokenProvider.generateToken(foundStaff.getWorkId(), "STAFF");

                // Set JWT in HttpOnly cookie
                setJwtCookie(response, token);

                return ResponseEntity.ok(new AuthResponse(
                        "Login successful",
                        "STAFF",
                        foundStaff.getWorkId(),
                        foundStaff.getFullName(),
                        foundStaff.getRole().toString()
                ));
            } else {
                return ResponseEntity.status(401).body(
                        new AuthResponse("Check credentials and Try again", "ERROR")
                );
            }

        } catch (Exception e) {
            return ResponseEntity.status(401).body(
                    new AuthResponse("Login failed: " + e.getMessage(), "ERROR")
            );
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Clear JWT cookie by setting max age to 0
        Cookie cookie = new Cookie("jwtToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new AuthResponse("Logout successful", null));
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("jwtToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Only send over HTTPS in production, set to true in prod only
        cookie.setPath("/");
        cookie.setMaxAge(3600); 
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }
}