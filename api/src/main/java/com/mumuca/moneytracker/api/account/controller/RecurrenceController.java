package com.mumuca.moneytracker.api.account.controller;

import com.mumuca.moneytracker.api.account.service.TransferService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;


/**
 * Controlador para gerenciar recorrências.
 * <p>
 * Funcionalidades:
 * <ul>
 * <li>Pegar recorrências;</li>
 * <li>Editar todas as recorrências de transferências repetidas;</li>
 * <li>Editar futuras recorrências de transferências repetidas;</li>
 * <li>Editar futuras recorrências de transferências parceladas;</li>
 * <li>Deletar recorrências de transferências futuras; ✅</li>
 * </ul>
 */
@RestController
@AllArgsConstructor
public class RecurrenceController {

    private final TransferService transferService;

    @DeleteMapping(path = "/v1/recurrences/{id}/installments/{installmentIndex}")
    public ResponseEntity<Void> deleteFutureTransfers(
            @PathVariable("id") String recurrenceId,
            @PathVariable("installmentIndex") Integer installmentIndex,
            @AuthenticationPrincipal Jwt jwt
    ) {
        transferService.deleteFutureTransfers(recurrenceId, installmentIndex, jwt.getSubject());

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
