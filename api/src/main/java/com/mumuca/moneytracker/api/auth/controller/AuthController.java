package com.mumuca.moneytracker.api.auth.controller;

import com.mumuca.moneytracker.api.auth.service.AuthService;
import com.mumuca.moneytracker.api.auth.dto.SignUpDTO;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping(path = "/v1/auth/sign-up")
    public ResponseEntity<Void> signUp(@Valid @RequestBody SignUpDTO signUpDTO) {
        authService.signUp(signUpDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }
}
