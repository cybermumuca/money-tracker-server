package com.mumuca.moneytracker.api.account.controller;

import com.mumuca.moneytracker.api.account.dto.*;
import com.mumuca.moneytracker.api.account.model.Status;
import com.mumuca.moneytracker.api.account.service.TransferService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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

    @GetMapping(path = "/v1/transfers/{id}")
    public ResponseEntity<RecurrenceDTO<TransferDTO>> getTransfer(
            @PathVariable("id") String transferId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        RecurrenceDTO<TransferDTO> transfer = transferService.getTransfer(transferId, jwt.getSubject());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(transfer);
    }

    @GetMapping(path = "/v1/transfers")
    public ResponseEntity<Page<RecurrenceDTO<TransferDTO>>> listTransfers(
        @RequestParam(value = "startDate", required = false, defaultValue = "#{T(java.time.LocalDate).now()}")
        LocalDate startDate,
        @RequestParam(value = "endDate", required = false, defaultValue = "#{T(java.time.LocalDate).now()}")
        LocalDate endDate,
        @PageableDefault(sort = "billingDate", size = 20) Pageable pageable,
        @RequestParam(value = "status", required = false, defaultValue = "ALL") Status status,
        @AuthenticationPrincipal Jwt jwt
    ) {
        Page<RecurrenceDTO<TransferDTO>> transferPage = transferService.listTransfers(
                startDate,
                endDate,
                pageable,
                status,
                jwt.getSubject()
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(transferPage);
    }

    @PatchMapping(path = "/v1/transfers/{id}/pay")
    public ResponseEntity<RecurrenceDTO<TransferDTO>> payTransfer(
            @PathVariable("id") String transferId,
            @RequestBody PayTransferDTO payTransferDTO,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String accountId = payTransferDTO.accountId();

        RecurrenceDTO<TransferDTO> transfer = transferService.payTransfer(transferId, accountId, jwt.getSubject());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(transfer);
    }
}
