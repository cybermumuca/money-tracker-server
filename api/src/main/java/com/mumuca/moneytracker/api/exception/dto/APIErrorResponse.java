package com.mumuca.moneytracker.api.exception.dto;

import java.time.LocalDateTime;

public record APIErrorResponse<T>(
        int status,
        LocalDateTime timestamp,
        String message,
        T details
) {}
