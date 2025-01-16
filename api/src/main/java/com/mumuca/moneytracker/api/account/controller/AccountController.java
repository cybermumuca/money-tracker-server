package com.mumuca.moneytracker.api.account.controller;

import com.mumuca.moneytracker.api.account.dto.AccountDTO;
import com.mumuca.moneytracker.api.account.dto.CreateAccountDTO;
import com.mumuca.moneytracker.api.account.dto.EditAccountDTO;
import com.mumuca.moneytracker.api.account.dto.WithdrawDTO;
import com.mumuca.moneytracker.api.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping(path = "/v1/accounts")
    public ResponseEntity<AccountDTO> createAccount(
            @Valid @RequestBody CreateAccountDTO createAccountDTO,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AccountDTO createdAccount = accountService.createAccount(createAccountDTO, jwt.getSubject());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdAccount);
    }

    @GetMapping(path = "/v1/accounts/{id}")
    public ResponseEntity<AccountDTO> getAccount(
            @PathVariable("id") String accountId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AccountDTO account = accountService.getAccount(accountId, jwt.getSubject());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(account);
    }

    @GetMapping(path = "/v1/accounts/active")
    public ResponseEntity<List<AccountDTO>> listActiveAccounts(@AuthenticationPrincipal Jwt jwt) {
        List<AccountDTO> activeAccounts = accountService.listActiveAccounts(jwt.getSubject());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(activeAccounts);
    }

    @GetMapping(path = "/v1/accounts/archived")
    public ResponseEntity<List<AccountDTO>> listArchivedAccounts(@AuthenticationPrincipal Jwt jwt) {
        List<AccountDTO> archivedAccounts = accountService.listArchivedAccounts(jwt.getSubject());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(archivedAccounts);
    }

    @PatchMapping(path = "/v1/accounts/{id}/archive")
    public ResponseEntity<Void> archiveAccount(
            @PathVariable("id") String accountId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        accountService.archiveAccount(accountId, jwt.getSubject());

        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }

    @PatchMapping(path = "/v1/accounts/{id}/unarchive")
    public ResponseEntity<Void> unarchiveAccount(
            @PathVariable("id") String accountId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        accountService.unarchiveAccount(accountId, jwt.getSubject());

        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }

    @PutMapping(path = "/v1/accounts/{id}")
    public ResponseEntity<AccountDTO> editAccount(
            @PathVariable("id") String accountId,
            @Valid @RequestBody EditAccountDTO editAccountDTO,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AccountDTO updatedAccount = accountService.editAccount(accountId, editAccountDTO, jwt.getSubject());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(updatedAccount);
    }

    @PostMapping(path = "/v1/accounts/{id}/withdraw")
    public ResponseEntity<AccountDTO> withdraw(
            @PathVariable("id") String accountId,
            @Valid @RequestBody WithdrawDTO withdrawDTO,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AccountDTO updatedAccount = accountService.withdraw(accountId, withdrawDTO, jwt.getSubject());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(updatedAccount);
    }

    @DeleteMapping(path = "/v1/accounts/{id}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable("id") String accountId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        accountService.deleteAccount(accountId, jwt.getSubject());

        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }
}
