package com.mumuca.moneytracker.api.auth.dto;

import com.mumuca.moneytracker.api.auth.model.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SignUpDTO(
        @NotBlank(message = "First name is required")
        String firstName,
        @NotBlank(message = "Last name is required")
        String lastName,
        @NotNull(message = "Gender is required")
        Gender gender,
        @NotNull(message = "Birth date is required")
        LocalDate birthDate,
        @Email(message = "Email is invalid")
        @NotBlank(message = "Email is required")
        String email,
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String password
) {}
