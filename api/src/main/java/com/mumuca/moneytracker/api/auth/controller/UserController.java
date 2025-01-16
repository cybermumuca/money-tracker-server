package com.mumuca.moneytracker.api.auth.controller;

import com.mumuca.moneytracker.api.auth.dto.UserDTO;
import com.mumuca.moneytracker.api.auth.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping(path = "/v1/me")
    public ResponseEntity<UserDTO> getProfile(@AuthenticationPrincipal Jwt jwt) {
        UserDTO userDTO = userService.getUser(jwt.getSubject());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(userDTO);
    }
}
