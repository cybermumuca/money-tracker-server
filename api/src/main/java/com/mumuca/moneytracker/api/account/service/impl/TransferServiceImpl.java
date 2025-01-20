package com.mumuca.moneytracker.api.account.service.impl;

import com.mumuca.moneytracker.api.account.dto.*;
import com.mumuca.moneytracker.api.account.exception.InvalidTransferDestinationException;
import com.mumuca.moneytracker.api.account.exception.InvalidTransferSourceException;
import com.mumuca.moneytracker.api.account.exception.TransferAlreadyPaidException;
import com.mumuca.moneytracker.api.account.exception.TransferNotPaidYetException;
import com.mumuca.moneytracker.api.account.model.*;
import com.mumuca.moneytracker.api.account.repository.AccountRepository;
import com.mumuca.moneytracker.api.account.repository.RecurrenceRepository;
import com.mumuca.moneytracker.api.account.repository.TransferRepository;
import com.mumuca.moneytracker.api.account.repository.specification.TransferSpecification;
import com.mumuca.moneytracker.api.account.service.TransferService;
import com.mumuca.moneytracker.api.auth.model.User;
import com.mumuca.moneytracker.api.exception.ResourceIsArchivedException;
import com.mumuca.moneytracker.api.exception.ResourceNotFoundException;
import com.mumuca.moneytracker.api.model.Money;
import com.mumuca.moneytracker.api.providers.CurrencyProvider;
import com.mumuca.moneytracker.api.providers.DateProvider;
import lombok.AllArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Service
@AllArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final RecurrenceRepository recurrenceRepository;
    private final CurrencyProvider currencyProvider;
    private final DateProvider dateProvider;

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
                    .interval(RecurrenceInterval.MONTHLY)
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
                    .installmentIndex(1)
                    .paid(registerUniqueTransferDTO.paidDate())
                    .recurrence(recurrence)
                    .build();

            transferRepository.save(transfer);

            boolean transferIsPaid = registerUniqueTransferDTO.paidDate() != null;

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
                    recurrence.getRecurrenceType(),
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
                            transfer.getPaid(),
                            1,
                            1,
                            transfer.getRecurrence().getId()
                    ))
            );
        } catch (CompletionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof ResourceNotFoundException) {
                throw (ResourceNotFoundException) cause;
            }

            if (cause instanceof ResourceIsArchivedException) {
                throw (ResourceIsArchivedException) cause;
            }

            throw new RuntimeException("An error occurred while registering an unique transfer: ", e);
        }
    }

    @Override
    @Transactional
    public RecurrenceDTO<TransferDTO> registerRepeatedTransfer(
            RegisterRepeatedTransferDTO registerRepeatedTransferDTO,
            String userId
    ) {
        try (ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Account> sourceAccountFuture = CompletableFuture.supplyAsync(() ->
                            accountRepository.findByIdAndUserId(registerRepeatedTransferDTO.fromAccount(), userId)
                                    .orElseThrow(() -> new ResourceNotFoundException("Source Account not found.")),
                    virtualThreadExecutor
            );

            CompletableFuture<Account> destinationAccountFuture = CompletableFuture.supplyAsync(() ->
                            accountRepository.findByIdAndUserId(registerRepeatedTransferDTO.toAccount(), userId)
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
                    .firstOccurrence(registerRepeatedTransferDTO.billingDate())
                    .interval(registerRepeatedTransferDTO.recurrenceInterval())
                    .transactionType(TransactionType.TRANSFER)
                    .recurrenceType(RecurrenceType.REPEATED)
                    .user(new User(userId))
                    .build();

            recurrenceRepository.save(recurrence);

            List<LocalDate> billingDates = dateProvider.generateDates(
                    registerRepeatedTransferDTO.billingDate(),
                    registerRepeatedTransferDTO.recurrenceInterval(),
                    registerRepeatedTransferDTO.numberOfRecurrences()
            );


            AtomicInteger index = new AtomicInteger(1);

            List<Transfer> transfers = billingDates
                    .stream()
                    .map((billingDate) -> {
                        Money transferValue = new Money(registerRepeatedTransferDTO.amount(), registerRepeatedTransferDTO.currency());

                        var paidDate = registerRepeatedTransferDTO.billingDate().equals(billingDate) ? registerRepeatedTransferDTO.paidDate() : null;

                        return Transfer.builder()
                                .title(registerRepeatedTransferDTO.title())
                                .description(registerRepeatedTransferDTO.description())
                                .sourceAccount(sourceAccount)
                                .destinationAccount(destinationAccount)
                                .value(transferValue)
                                .billingDate(billingDate)
                                .installmentIndex(index.getAndIncrement())
                                .paid(paidDate)
                                .recurrence(recurrence)
                                .build();
                    })
                    .toList();

            transferRepository.saveAll(transfers);

            boolean transferIsPaid = registerRepeatedTransferDTO.paidDate() != null;

            if (transferIsPaid) {
                handleCurrencyConversions(transfers.getFirst(), sourceAccount, destinationAccount);
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
                    recurrence.getRecurrenceType(),
                    transfers
                            .stream()
                            .map((transfer) -> new TransferDTO(
                                    transfer.getId(),
                                    transfer.getTitle(),
                                    transfer.getDescription(),
                                    sourceAccountDTO,
                                    destinationAccountDTO,
                                    transfer.getValue().getAmount(),
                                    transfer.getValue().getCurrency(),
                                    transfer.getBillingDate(),
                                    transfer.isPaid(),
                                    transfer.getPaid(),
                                    transfer.getInstallmentIndex(),
                                    transfers.size(),
                                    transfer.getRecurrence().getId()
                            ))
                            .toList()
            );
        } catch (CompletionException e) {
            Throwable cause = e.getCause();

            if (cause instanceof ResourceNotFoundException) {
                throw (ResourceNotFoundException) cause;
            }

            if (cause instanceof ResourceIsArchivedException) {
                throw (ResourceIsArchivedException) cause;
            }

            throw new RuntimeException("An error occurred while registering an unique transfer: ", e);
        }
    }

    @Override
    public RecurrenceDTO<TransferDTO> getTransfer(String transferId, String userId) {
        Recurrence recurrence = recurrenceRepository
                .findByTransferIdAndUserId(transferId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found."));

        List<Transfer> orderedTransfers = transferRepository.findTransfersByRecurrenceId(recurrence.getId());

        int transferIndex = IntStream.range(0, orderedTransfers.size())
                .filter(i -> orderedTransfers.get(i).getId().equals(transferId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found in recurrence.")) + 1;

        Transfer transfer = recurrence.getTransfers().getFirst();

        Account sourceAccount = transfer.getSourceAccount();

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

        Account destinationAccount = transfer.getDestinationAccount();

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

        TransferDTO transferDTO = new TransferDTO(
                transfer.getId(),
                transfer.getTitle(),
                transfer.getDescription(),
                sourceAccountDTO,
                destinationAccountDTO,
                transfer.getValue().getAmount(),
                transfer.getValue().getCurrency(),
                transfer.getBillingDate(),
                transfer.isPaid(),
                transfer.getPaid(),
                transferIndex,
                orderedTransfers.size(),
                transfer.getRecurrence().getId()
        );

        return new RecurrenceDTO<TransferDTO>(
                recurrence.getId(),
                recurrence.getInterval(),
                recurrence.getFirstOccurrence(),
                recurrence.getTransactionType(),
                recurrence.getRecurrenceType(),
                List.of(transferDTO)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecurrenceDTO<TransferDTO>> listTransfers(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable,
            Status status,
            String userId
    ) {
        Specification<Transfer> transferSpec = TransferSpecification
                .withFilters(userId, startDate, endDate, status);

        Page<Transfer> transfers = transferRepository.findAll(transferSpec, pageable);

        return transfers
                .map(transfer -> {
                    Recurrence recurrence = transfer.getRecurrence();

                    int totalTransfers = transferRepository.countTransfersByRecurrenceId(recurrence.getId());

                    Account sourceAccount = transfer.getSourceAccount();

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

                    Account destinationAccount = transfer.getDestinationAccount();

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

                    TransferDTO transferDTO = new TransferDTO(
                            transfer.getId(),
                            transfer.getTitle(),
                            transfer.getDescription(),
                            sourceAccountDTO,
                            destinationAccountDTO,
                            transfer.getValue().getAmount(),
                            transfer.getValue().getCurrency(),
                            transfer.getBillingDate(),
                            transfer.isPaid(),
                            transfer.getPaid(),
                            transfer.getInstallmentIndex(),
                            totalTransfers,
                            transfer.getRecurrence().getId()
                    );


                    return new RecurrenceDTO<TransferDTO>(
                            recurrence.getId(),
                            recurrence.getInterval(),
                            recurrence.getFirstOccurrence(),
                            recurrence.getTransactionType(),
                            recurrence.getRecurrenceType(),
                            List.of(transferDTO)
                    );
                });
    }

    @Override
    @Transactional
    public RecurrenceDTO<TransferDTO> payTransfer(String transferId, PayTransferDTO payTransferDTO, String userId) {
        String accountId = payTransferDTO.accountId();

        Recurrence recurrence = recurrenceRepository
                .findByTransferIdAndUserId(transferId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found."));

        Transfer transferToPay = recurrence.getTransfers().getFirst();

        Account accountToBePaid = transferToPay.getDestinationAccount();

        if (accountToBePaid == null) {
            throw new InvalidTransferDestinationException();
        }

        if (transferToPay.isPaid()) {
            throw new TransferAlreadyPaidException();
        }

        Account accountToPay;

        if (accountId == null) {
            accountToPay = transferToPay.getSourceAccount();

            if (accountToPay == null) {
                throw new InvalidTransferSourceException();
            }
        } else {
            accountToPay = accountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        }

        if (accountToPay.isArchived()) {
            throw new ResourceIsArchivedException("Unable to pay transfer if source account is archived.");
        }

        handleCurrencyConversions(transferToPay, accountToPay, accountToBePaid);

        transferToPay.setSourceAccount(accountToPay);

        LocalDate paidAt = payTransferDTO.paidDate() != null ? payTransferDTO.paidDate() : LocalDate.now();

        transferToPay.setPaid(paidAt);

        transferRepository.save(transferToPay);

        accountRepository.saveAll(List.of(accountToPay, accountToBePaid));

        AccountDTO sourceAccountDTO = new AccountDTO(
                accountToPay.getId(),
                accountToPay.getName(),
                accountToPay.getColor(),
                accountToPay.getIcon(),
                accountToPay.getType(),
                accountToPay.getBalance().getAmount(),
                accountToPay.getBalance().getCurrency(),
                accountToPay.isArchived()
        );

        AccountDTO destinationAccountDTO = new AccountDTO(
                accountToBePaid.getId(),
                accountToBePaid.getName(),
                accountToBePaid.getColor(),
                accountToBePaid.getIcon(),
                accountToBePaid.getType(),
                accountToBePaid.getBalance().getAmount(),
                accountToBePaid.getBalance().getCurrency(),
                accountToBePaid.isArchived()
        );

        int installmentsNumber = 1;

        if (recurrence.getRecurrenceType() != RecurrenceType.UNIQUE) {
            installmentsNumber = transferRepository.countTransfersByRecurrenceId(recurrence.getId());
        }

        TransferDTO transferDTO = new TransferDTO(
                transferToPay.getId(),
                transferToPay.getTitle(),
                transferToPay.getDescription(),
                sourceAccountDTO,
                destinationAccountDTO,
                transferToPay.getValue().getAmount(),
                transferToPay.getValue().getCurrency(),
                transferToPay.getBillingDate(),
                transferToPay.isPaid(),
                transferToPay.getPaid(),
                transferToPay.getInstallmentIndex(),
                installmentsNumber,
                transferToPay.getRecurrence().getId()
        );

        return new RecurrenceDTO<TransferDTO>(
                recurrence.getId(),
                recurrence.getInterval(),
                recurrence.getFirstOccurrence(),
                recurrence.getTransactionType(),
                recurrence.getRecurrenceType(),
                List.of(transferDTO)
        );
    }

    @Override
    @Transactional
    public RecurrenceDTO<TransferDTO> unpayTransfer(String transferId, String userId) {
        Recurrence recurrence = recurrenceRepository
                .findByTransferIdAndUserId(transferId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found."));

        Transfer transferToUnpay = recurrence.getTransfers().getFirst();

        if (!transferToUnpay.isPaid()) {
            throw new TransferNotPaidYetException();
        }

        var accountToDeposit = transferToUnpay.getSourceAccount();

        if (accountToDeposit == null) {
            throw new InvalidTransferSourceException("Transfer Source Account not found.");
        }

        Account accountToWithdraw = transferToUnpay.getDestinationAccount();

        if (accountToWithdraw == null) {
            throw new InvalidTransferDestinationException("Transfer Destination Account not found.");
        }

        handleCurrencyConversions(transferToUnpay, accountToWithdraw, accountToDeposit);

        transferToUnpay.setPaid(null);

        transferRepository.save(transferToUnpay);

        accountRepository.saveAll(List.of(accountToWithdraw, accountToDeposit));

        AccountDTO sourceAccountDTO = new AccountDTO(
                accountToDeposit.getId(),
                accountToDeposit.getName(),
                accountToDeposit.getColor(),
                accountToDeposit.getIcon(),
                accountToDeposit.getType(),
                accountToDeposit.getBalance().getAmount(),
                accountToDeposit.getBalance().getCurrency(),
                accountToDeposit.isArchived()
        );

        AccountDTO destinationAccountDTO = new AccountDTO(
                accountToWithdraw.getId(),
                accountToWithdraw.getName(),
                accountToWithdraw.getColor(),
                accountToWithdraw.getIcon(),
                accountToWithdraw.getType(),
                accountToWithdraw.getBalance().getAmount(),
                accountToWithdraw.getBalance().getCurrency(),
                accountToWithdraw.isArchived()
        );

        int installmentsNumber = 1;

        if (recurrence.getRecurrenceType() != RecurrenceType.UNIQUE) {
            installmentsNumber = transferRepository.countTransfersByRecurrenceId(recurrence.getId());
        }

        TransferDTO transferDTO = new TransferDTO(
                transferToUnpay.getId(),
                transferToUnpay.getTitle(),
                transferToUnpay.getDescription(),
                sourceAccountDTO,
                destinationAccountDTO,
                transferToUnpay.getValue().getAmount(),
                transferToUnpay.getValue().getCurrency(),
                transferToUnpay.getBillingDate(),
                transferToUnpay.isPaid(),
                transferToUnpay.getPaid(),
                transferToUnpay.getInstallmentIndex(),
                installmentsNumber,
                transferToUnpay.getRecurrence().getId()
        );

        return new RecurrenceDTO<TransferDTO>(
                recurrence.getId(),
                recurrence.getInterval(),
                recurrence.getFirstOccurrence(),
                recurrence.getTransactionType(),
                recurrence.getRecurrenceType(),
                List.of(transferDTO)
        );
    }
}
