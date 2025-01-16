package com.mumuca.moneytracker.api.auth.dto;

public record SignInDTO(
        String email,
        String password
) {}