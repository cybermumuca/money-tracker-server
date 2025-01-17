package com.mumuca.moneytracker.api.account.controller;

import com.mumuca.moneytracker.api.account.dto.RecurrenceDTO;
import com.mumuca.moneytracker.api.account.dto.RegisterRepeatedTransferDTO;
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

/**
* Controlador para gerenciar transferências.
* <p>
* Funcionalidades:
* <ul>
* <li>Registrar uma transferência única;</li>
* <li>Registrar uma transferência repetida;</li>
* <li>Registrar uma transferência parcelada;</li>
* <li>Editar uma transferência (única, parcelada ou repetida);</li>
* <li>Editar futuras transferências repetidas;</li>
* <li>Editar futuras transferências parceladas;</li>
* <li>Editar todas as recorrências de transferências repetidas;</li>
* <li>Pegar transferência;</li>
* <li>Listar transferências;</li>
* <li>Pagar transferência;</li>
* <li>"Despagar" transferência;</li>
* <li>Deletar transferência (única, parcelada ou repetida);</li>
* <li>Deletar transferências futuras;</li>
* <li>Pesquisar transferências;</li>
* </ul>
*/
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

    @PostMapping(path = "/v1/transfers/repeated")
    public ResponseEntity<RecurrenceDTO<TransferDTO>> registerRepeatedTransfer(
            @Valid @RequestBody RegisterRepeatedTransferDTO registerRepeatedTransferDTO,
            @AuthenticationPrincipal Jwt jwt
    ) {
        RecurrenceDTO<TransferDTO> transfer = transferService
                .registerRepeatedTransfer(registerRepeatedTransferDTO, jwt.getSubject());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(transfer);
    }

}
