package com.mumuca.moneytracker.api.auth.controller;

import com.mumuca.moneytracker.api.auth.dto.SignInDTO;
import com.mumuca.moneytracker.api.auth.dto.SignedIn;
import com.mumuca.moneytracker.api.auth.service.AuthService;
import com.mumuca.moneytracker.api.auth.dto.SignUpDTO;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

    @PostMapping(path = "/v1/auth/sign-in")
    public ResponseEntity<SignedIn> signIn(@RequestBody SignInDTO signInDTO) {
        SignedIn signInResponse = authService.signIn(signInDTO);

        HttpCookie jwtCookie = ResponseCookie.from("jwt", signInResponse.accessToken())
                .path("/")
                .httpOnly(true)
                .secure(true)
                .maxAge(signInResponse.expiresIn())
                .sameSite("None")
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(signInResponse);
    }

    @PostMapping(path = "/v1/auth/sign-out")
    public ResponseEntity<Void> signOut(@AuthenticationPrincipal Jwt jwt) {
        authService.signOut(jwt.getTokenValue(), jwt.getExpiresAt());

        HttpCookie jwtCookie = ResponseCookie.from("jwt", "")
                .path("/")
                .httpOnly(true)
                .secure(true)
                .maxAge(0)
                .sameSite("None")
                .build();

        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .build();
    }
}
