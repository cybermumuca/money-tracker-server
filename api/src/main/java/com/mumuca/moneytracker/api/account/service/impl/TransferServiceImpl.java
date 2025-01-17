package com.mumuca.moneytracker.api.account.service.impl;

import com.mumuca.moneytracker.api.account.dto.AccountDTO;
import com.mumuca.moneytracker.api.account.dto.RecurrenceDTO;
import com.mumuca.moneytracker.api.account.dto.RegisterUniqueTransferDTO;
import com.mumuca.moneytracker.api.account.dto.TransferDTO;
import com.mumuca.moneytracker.api.account.model.*;
import com.mumuca.moneytracker.api.account.repository.AccountRepository;
import com.mumuca.moneytracker.api.account.repository.RecurrenceRepository;
import com.mumuca.moneytracker.api.account.repository.TransferRepository;
import com.mumuca.moneytracker.api.account.service.TransferService;
import com.mumuca.moneytracker.api.auth.model.User;
import com.mumuca.moneytracker.api.exception.ResourceIsArchivedException;
import com.mumuca.moneytracker.api.exception.ResourceNotFoundException;
import com.mumuca.moneytracker.api.model.Money;
import com.mumuca.moneytracker.api.providers.CurrencyProvider;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@AllArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final RecurrenceRepository recurrenceRepository;
    private final CurrencyProvider currencyProvider;

    private void handleCurrencyConversions(
        Transfer transfer,
        Account sourceAccount,
        Account destinationAccount
    ) {
        boolean sourceAccountCurrencyMatchesTransferCurrency =
                sourceAccount.getBalance().equals(transfer.getValue());

        boolean destinationAccountCurrencyMatchesTransferCurrency =
                destinationAccount.getBalance().equals(transfer.getValue());

        if (sourceAccountCurrencyMatchesTransferCurrency && destinationAccountCurrencyMatchesTransferCurrency) {
            sourceAccount.withdraw(transfer.getValue());
            destinationAccount.deposit(transfer.getValue());
        }

        if (sourceAccountCurrencyMatchesTransferCurrency && !destinationAccountCurrencyMatchesTransferCurrency) {
            BigDecimal convertedAmountToAdd = this.currencyProvider.convertCurrency(
                    transfer.getValue().getAmount(),
                    transfer.getValue().getCurrency(),
                    destinationAccount.getBalance().getCurrency()
            );

            sourceAccount.withdraw(transfer.getValue());
            destinationAccount.deposit(convertedAmountToAdd);
        }

        if (!sourceAccountCurrencyMatchesTransferCurrency && destinationAccountCurrencyMatchesTransferCurrency) {
            BigDecimal convertedAmountToSubtract = this.currencyProvider.convertCurrency(
                    transfer.getValue().getAmount(),
                    transfer.getValue().getCurrency(),
                    sourceAccount.getBalance().getCurrency()
            );

            sourceAccount.withdraw(convertedAmountToSubtract);
            destinationAccount.deposit(transfer.getValue());
        }

        if (!sourceAccountCurrencyMatchesTransferCurrency && !destinationAccountCurrencyMatchesTransferCurrency) {
            BigDecimal convertedAmountToSubtract = this.currencyProvider.convertCurrency(
                    transfer.getValue().getAmount(),
                    transfer.getValue().getCurrency(),
                    sourceAccount.getBalance().getCurrency()
            );

            BigDecimal convertedAmountToAdd = this.currencyProvider.convertCurrency(
                    transfer.getValue().getAmount(),
                    transfer.getValue().getCurrency(),
                    destinationAccount.getBalance().getCurrency()
            );

            sourceAccount.withdraw(convertedAmountToSubtract);
            destinationAccount.deposit(convertedAmountToAdd);
        }
    }

    @Override
    @Transactional
    public RecurrenceDTO<TransferDTO> registerUniqueTransfer(
            RegisterUniqueTransferDTO registerUniqueTransferDTO,
            String userId
    ) {
        try (ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Account> sourceAccountFuture = CompletableFuture.supplyAsync(() ->
                            accountRepository.findByIdAndUserId(registerUniqueTransferDTO.fromAccount(), userId)
                                    .orElseThrow(() -> new ResourceNotFoundException("Source Account not found.")),
                    virtualThreadExecutor
            );

            CompletableFuture<Account> destinationAccountFuture = CompletableFuture.supplyAsync(() ->
                            accountRepository.findByIdAndUserId(registerUniqueTransferDTO.toAccount(), userId)
                                    .orElseThrow(() -> new ResourceNotFoundException("Destination Account not found.")),
                    virtualThreadExecutor
            );

            Account sourceAccount = sourceAccountFuture.join();
            Account destinationAccount = destinationAccountFuture.join();

            if (sourceAccount.isArchived()) {
                throw new ResourceIsArchivedException("Source Account is archived.");
            }
            if (destinationAccount.isArchived()) {
                throw new ResourceIsArchivedException("Destination Account is archived.");
            }

            Recurrence recurrence = Recurrence.builder()
                    .firstOccurrence(registerUniqueTransferDTO.billingDate())
                    .interval(Interval.MONTHLY)
                    .transactionType(TransactionType.TRANSFER)
                    .recurrenceType(RecurrenceType.UNIQUE)
                    .user(new User(userId))
                    .build();

            recurrenceRepository.save(recurrence);

            Money transferValue = new Money(registerUniqueTransferDTO.amount(), registerUniqueTransferDTO.currency());

            Transfer transfer = Transfer.builder()
                    .title(registerUniqueTransferDTO.title())
                    .description(registerUniqueTransferDTO.description())
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(transferValue)
                    .billingDate(registerUniqueTransferDTO.billingDate())
                    .paid(registerUniqueTransferDTO.paid())
                    .recurrence(recurrence)
                    .build();

            transferRepository.save(transfer);

            boolean transferIsPaid = registerUniqueTransferDTO.paid();

            if (transferIsPaid) {
                handleCurrencyConversions(transfer, sourceAccount, destinationAccount);
            }

            accountRepository.saveAll(List.of(sourceAccount, destinationAccount));

            AccountDTO sourceAccountDTO = new AccountDTO(
                    sourceAccount.getId(),
                    sourceAccount.getName(),
                    sourceAccount.getColor(),
                    sourceAccount.getIcon(),
                    sourceAccount.getType(),
                    sourceAccount.getBalance().getAmount(),
                    sourceAccount.getBalance().getCurrency(),
                    sourceAccount.isArchived()
            );

            AccountDTO destinationAccountDTO = new AccountDTO(
                    destinationAccount.getId(),
                    destinationAccount.getName(),
                    destinationAccount.getColor(),
                    destinationAccount.getIcon(),
                    destinationAccount.getType(),
                    destinationAccount.getBalance().getAmount(),
                    destinationAccount.getBalance().getCurrency(),
                    destinationAccount.isArchived()
            );

            return new RecurrenceDTO<TransferDTO>(
                    recurrence.getId(),
                    recurrence.getInterval(),
                    recurrence.getFirstOccurrence(),
                    recurrence.getTransactionType(),
                    List.of(new TransferDTO(
                            transfer.getId(),
                            transfer.getTitle(),
                            transfer.getDescription(),
                            sourceAccountDTO,
                            destinationAccountDTO,
                            transfer.getValue().getAmount(),
                            transfer.getValue().getCurrency(),
                            transfer.getBillingDate(),
                            transfer.isPaid(),
                            transfer.getRecurrence().getId()
                    ))
            );
        }
    }
}
