package com.ehr.dto;

import com.ehr.models.Staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StaffCreateDto {
    @NotBlank(message = "work id is required for staff accounts")
    private String workId;

    private String firstName;
    private String lastName;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
//    @Pattern(
//            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
//            message = "Password must contain at least one digit, one lowercase letter, one uppercase letter, one special character, and no whitespace"
//    ) for prod
    private String password;

    @NotNull(message = "Role must be provided")
    private Staff.Role role;
}
