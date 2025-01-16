package com.mumuca.moneytracker.api.util;

import com.mumuca.moneytracker.api.exception.dto.APIErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public class HttpUtils {
    public static <T> ResponseEntity<APIErrorResponse<T>> buildErrorResponse(
            HttpStatus status,
            String title,
            T details
    ) {
        APIErrorResponse<T> errorResponse = new APIErrorResponse<>(
                status.value(),
                LocalDateTime.now(),
                title,
                details
        );

        return ResponseEntity.status(status).body(errorResponse);
    }
}
