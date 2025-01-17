package com.mumuca.moneytracker.api.account.controller;

import com.mumuca.moneytracker.api.account.dto.RecurrenceDTO;
import com.mumuca.moneytracker.api.account.dto.RegisterUniqueTransferDTO;
import com.mumuca.moneytracker.api.account.dto.TransferDTO;
import com.mumuca.moneytracker.api.account.service.TransferService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping(path = "/v1/transfers/unique")
    public ResponseEntity<RecurrenceDTO<TransferDTO>> registerUniqueTransfer(
            @Valid @RequestBody RegisterUniqueTransferDTO registerUniqueTransferDTO,
            @AuthenticationPrincipal Jwt jwt
    ) {
        RecurrenceDTO<TransferDTO> transfer = transferService
                .registerUniqueTransfer(registerUniqueTransferDTO, jwt.getSubject());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transfer);
    }
}
