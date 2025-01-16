package com.mumuca.moneytracker.api.auth.dto;

import com.mumuca.moneytracker.api.auth.model.Gender;

public record UserDTO(
        String id,
        String email,
        String firstName,
        String lastName,
        String picture,
        Gender gender,
        int age
) {}
