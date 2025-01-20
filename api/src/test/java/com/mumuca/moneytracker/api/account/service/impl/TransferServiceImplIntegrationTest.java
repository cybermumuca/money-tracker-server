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
import com.mumuca.moneytracker.api.auth.model.User;
import com.mumuca.moneytracker.api.auth.repository.UserRepository;
import com.mumuca.moneytracker.api.exception.ResourceIsArchivedException;
import com.mumuca.moneytracker.api.exception.ResourceNotFoundException;
import com.mumuca.moneytracker.api.model.Money;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


import static com.mumuca.moneytracker.api.testutil.EntityGeneratorUtil.*;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("TransferServiceImpl Integration Tests")
class TransferServiceImplIntegrationTest {

    @Autowired
    private TransferServiceImpl sut;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private RecurrenceRepository recurrenceRepository;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("registerUniqueTransfer tests")
    class RegisterUniqueTransferTests {
        @Test
        @DisplayName("should be able to register a transfer of type \"UNIQUE\" that has been paid")
        void shouldBeAbleToRegisterATransferOfTypeUniqueThatHasBeenPaid() {
            // Arrange
            User user = createUser();
            userRepository.save(user);


            BigDecimal sourceAccountBalance = BigDecimal.valueOf(50000);
            String sourceAccountCurrency = "BRL";
            Money sourceAccountMoney = new Money(sourceAccountBalance, sourceAccountCurrency);
            Account sourceAccount = createAccount();
            sourceAccount.setBalance(sourceAccountMoney);
            sourceAccount.setUser(user);

            BigDecimal destinationAccountBalance = BigDecimal.valueOf(0);
            String destinationAccountCurrency = "BRL";
            Money destinationAccountMoney = new Money(destinationAccountBalance, destinationAccountCurrency);
            Account destinationAccount = createAccount();
            destinationAccount.setBalance(destinationAccountMoney);
            destinationAccount.setUser(user);

            user.setAccounts(List.of(sourceAccount, destinationAccount));

            accountRepository.saveAll(List.of(sourceAccount, destinationAccount));


            BigDecimal amountToTransfer = BigDecimal.valueOf(10000);
            String transferCurrency = destinationAccountCurrency;

            var today = LocalDate.now();

            LocalDate billingDate = today.plusDays(3);

            RegisterUniqueTransferDTO registerUniqueTransferDTO = new RegisterUniqueTransferDTO(
                    "Test Transfer",
                    "Test Description",
                    amountToTransfer,
                    transferCurrency,
                    sourceAccount.getId(),
                    destinationAccount.getId(),
                    billingDate,
                    today
            );

            // Act
            RecurrenceDTO<TransferDTO> result = sut.registerUniqueTransfer(registerUniqueTransferDTO, user.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isNotNull();
            assertThat(result.interval()).isEqualTo(RecurrenceInterval.MONTHLY);
            assertThat(result.firstOccurrence()).isEqualTo(billingDate);
            assertThat(result.transactionType()).isEqualTo(TransactionType.TRANSFER);
            assertThat(result.recurrenceType()).isEqualTo(RecurrenceType.UNIQUE);
            assertThat(result.recurrences()).hasSize(1);

            TransferDTO transferResult = result.recurrences().getFirst();

            assertThat(transferResult).isNotNull();
            assertThat(transferResult.id()).isNotNull();
            assertThat(transferResult.title()).isEqualTo(registerUniqueTransferDTO.title());
            assertThat(transferResult.description()).isEqualTo(registerUniqueTransferDTO.description());
            assertThat(transferResult.value()).isEqualTo(amountToTransfer);
            assertThat(transferResult.currency()).isEqualTo(transferCurrency);
            assertThat(transferResult.billingDate()).isEqualTo(billingDate);
            assertThat(transferResult.paid()).isTrue();
            assertThat(transferResult.paidDate()).isEqualTo(today);
            assertThat(transferResult.installmentIndex()).isEqualTo(1);
            assertThat(transferResult.installments()).isEqualTo(1);
            assertThat(transferResult.recurrenceId()).isEqualTo(result.id());

            AccountDTO sourceAccountTransferResult = transferResult.fromAccount();

            assertThat(sourceAccountTransferResult).isNotNull();
            assertThat(sourceAccountTransferResult.id()).isEqualTo(sourceAccount.getId());
            assertThat(sourceAccountTransferResult.name()).isEqualTo(sourceAccount.getName());
            assertThat(sourceAccountTransferResult.color()).isEqualTo(sourceAccount.getColor());
            assertThat(sourceAccountTransferResult.icon()).isEqualTo(sourceAccount.getIcon());
            assertThat(sourceAccountTransferResult.type()).isEqualTo(sourceAccount.getType());
            assertThat(sourceAccountTransferResult.balance()).isEqualByComparingTo(sourceAccountBalance.subtract(amountToTransfer));
            assertThat(sourceAccountTransferResult.currency()).isEqualTo(sourceAccountCurrency);
            assertThat(sourceAccountTransferResult.isArchived()).isEqualTo(false);

            AccountDTO destinationAccountTransferResult = transferResult.toAccount();

            assertThat(destinationAccountTransferResult).isNotNull();
            assertThat(destinationAccountTransferResult.id()).isEqualTo(destinationAccount.getId());
            assertThat(destinationAccountTransferResult.name()).isEqualTo(destinationAccount.getName());
            assertThat(destinationAccountTransferResult.color()).isEqualTo(destinationAccount.getColor());
            assertThat(destinationAccountTransferResult.icon()).isEqualTo(destinationAccount.getIcon());
            assertThat(destinationAccountTransferResult.type()).isEqualTo(destinationAccount.getType());
            assertThat(destinationAccountTransferResult.balance()).isEqualByComparingTo(destinationAccountBalance.add(amountToTransfer));
            assertThat(destinationAccountTransferResult.currency()).isEqualTo(transferCurrency);
            assertThat(destinationAccountTransferResult.isArchived()).isEqualTo(false);

            var optionalRecurrenceInDatabase = recurrenceRepository.findById(result.id());

            assertThat(optionalRecurrenceInDatabase).isPresent();

            var recurrenceInDatabase = optionalRecurrenceInDatabase.get();

            assertThat(recurrenceInDatabase.getId()).isEqualTo(result.id());
            assertThat(recurrenceInDatabase.getInterval()).isEqualTo(result.interval());
            assertThat(recurrenceInDatabase.getFirstOccurrence()).isEqualTo(result.firstOccurrence());
            assertThat(recurrenceInDatabase.getTransactionType()).isEqualTo(result.transactionType());
            assertThat(recurrenceInDatabase.getRecurrenceType()).isEqualTo(result.recurrenceType());
            assertThat(recurrenceInDatabase.getUser().getId()).isEqualTo(user.getId());

            var optionalTransferInDatabase = transferRepository.findById(transferResult.id());

            assertThat(optionalTransferInDatabase).isPresent();

            var transferInDatabase = optionalTransferInDatabase.get();

            assertThat(transferInDatabase.getId()).isEqualTo(transferResult.id());
            assertThat(transferInDatabase.getTitle()).isEqualTo(transferResult.title());
            assertThat(transferInDatabase.getDescription()).isEqualTo(transferResult.description());
            assertThat(transferInDatabase.getSourceAccount().getId()).isEqualTo(sourceAccount.getId());
            assertThat(transferInDatabase.getDestinationAccount().getId()).isEqualTo(destinationAccount.getId());
            assertThat(transferInDatabase.getBillingDate()).isEqualTo(transferResult.billingDate());
            assertThat(transferInDatabase.getValue().getAmount()).isEqualByComparingTo(transferResult.value());
            assertThat(transferInDatabase.getValue().getCurrency()).isEqualTo(transferResult.currency());
            assertThat(transferInDatabase.isPaid()).isEqualTo(transferResult.paid());
            assertThat(transferInDatabase.getPaid()).isEqualTo(transferResult.paidDate());
            assertThat(transferInDatabase.getRecurrence().getId()).isEqualTo(result.id());

            var optionalSourceAccountInDatabase = accountRepository.findById(sourceAccount.getId());

            assertThat(optionalSourceAccountInDatabase).isPresent();

            var sourceAccountInDatabase = optionalSourceAccountInDatabase.get();

            assertThat(sourceAccountInDatabase.getId()).isEqualTo(sourceAccount.getId());
            assertThat(sourceAccountInDatabase.getName()).isEqualTo(sourceAccount.getName());
            assertThat(sourceAccountInDatabase.getColor()).isEqualTo(sourceAccount.getColor());
            assertThat(sourceAccountInDatabase.getIcon()).isEqualTo(sourceAccount.getIcon());
            assertThat(sourceAccountInDatabase.getType()).isEqualTo(sourceAccount.getType());
            assertThat(sourceAccountInDatabase.getBalance().getAmount()).isEqualByComparingTo(sourceAccountBalance.subtract(amountToTransfer));
            assertThat(sourceAccountInDatabase.getBalance().getCurrency()).isEqualTo(sourceAccountCurrency);
            assertThat(sourceAccountInDatabase.isArchived()).isEqualTo(false);

            var optionalDestinationAccountInDatabase = accountRepository.findById(destinationAccount.getId());

            assertThat(optionalDestinationAccountInDatabase).isPresent();

            var destinationAccountInDatabase = optionalDestinationAccountInDatabase.get();

            assertThat(destinationAccountInDatabase.getId()).isEqualTo(destinationAccount.getId());
            assertThat(destinationAccountInDatabase.getName()).isEqualTo(destinationAccount.getName());
            assertThat(destinationAccountInDatabase.getColor()).isEqualTo(destinationAccount.getColor());
            assertThat(destinationAccountInDatabase.getIcon()).isEqualTo(destinationAccount.getIcon());
            assertThat(destinationAccountInDatabase.getType()).isEqualTo(destinationAccount.getType());
            assertThat(destinationAccountInDatabase.getBalance().getAmount()).isEqualByComparingTo(destinationAccountBalance.add(amountToTransfer));
            assertThat(destinationAccountInDatabase.getBalance().getCurrency()).isEqualTo(transferCurrency);
            assertThat(destinationAccountInDatabase.isArchived()).isEqualTo(false);
        }

        @Test
        @DisplayName("should be able to register a transfer of type \"UNIQUE\" that has not been paid")
        void shouldBeAbleToRegisterATransferOfTypeUniqueThatHasNotBeenPaid() {
            /// Arrange
            User user = createUser();
            userRepository.save(user);


            BigDecimal sourceAccountBalance = BigDecimal.valueOf(50000);
            String sourceAccountCurrency = "BRL";
            Money sourceAccountMoney = new Money(sourceAccountBalance, sourceAccountCurrency);
            Account sourceAccount = createAccount();
            sourceAccount.setBalance(sourceAccountMoney);
            sourceAccount.setUser(user);

            BigDecimal destinationAccountBalance = BigDecimal.valueOf(0);
            String destinationAccountCurrency = "BRL";
            Money destinationAccountMoney = new Money(destinationAccountBalance, destinationAccountCurrency);
            Account destinationAccount = createAccount();
            destinationAccount.setBalance(destinationAccountMoney);
            destinationAccount.setUser(user);

            user.setAccounts(List.of(sourceAccount, destinationAccount));

            accountRepository.saveAll(List.of(sourceAccount, destinationAccount));


            BigDecimal amountToTransfer = BigDecimal.valueOf(10000);
            String transferCurrency = destinationAccountCurrency;

            var today = LocalDate.now();

            LocalDate billingDate = today.plusDays(3);

            RegisterUniqueTransferDTO registerUniqueTransferDTO = new RegisterUniqueTransferDTO(
                    "Test Transfer",
                    "Test Description",
                    amountToTransfer,
                    transferCurrency,
                    sourceAccount.getId(),
                    destinationAccount.getId(),
                    billingDate,
                    null
            );

            // Act
            RecurrenceDTO<TransferDTO> result = sut.registerUniqueTransfer(registerUniqueTransferDTO, user.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isNotNull();
            assertThat(result.interval()).isEqualTo(RecurrenceInterval.MONTHLY);
            assertThat(result.firstOccurrence()).isEqualTo(billingDate);
            assertThat(result.transactionType()).isEqualTo(TransactionType.TRANSFER);
            assertThat(result.recurrenceType()).isEqualTo(RecurrenceType.UNIQUE);
            assertThat(result.recurrences()).hasSize(1);

            TransferDTO transferResult = result.recurrences().getFirst();

            assertThat(transferResult).isNotNull();
            assertThat(transferResult.id()).isNotNull();
            assertThat(transferResult.title()).isEqualTo(registerUniqueTransferDTO.title());
            assertThat(transferResult.description()).isEqualTo(registerUniqueTransferDTO.description());
            assertThat(transferResult.value()).isEqualTo(amountToTransfer);
            assertThat(transferResult.currency()).isEqualTo(transferCurrency);
            assertThat(transferResult.billingDate()).isEqualTo(billingDate);
            assertThat(transferResult.installmentIndex()).isEqualTo(1);
            assertThat(transferResult.installments()).isEqualTo(1);
            assertThat(transferResult.paid()).isFalse();
            assertThat(transferResult.paidDate()).isNull();
            assertThat(transferResult.recurrenceId()).isEqualTo(result.id());

            AccountDTO sourceAccountTransferResult = transferResult.fromAccount();

            assertThat(sourceAccountTransferResult).isNotNull();
            assertThat(sourceAccountTransferResult.id()).isEqualTo(sourceAccount.getId());
            assertThat(sourceAccountTransferResult.name()).isEqualTo(sourceAccount.getName());
            assertThat(sourceAccountTransferResult.color()).isEqualTo(sourceAccount.getColor());
            assertThat(sourceAccountTransferResult.icon()).isEqualTo(sourceAccount.getIcon());
            assertThat(sourceAccountTransferResult.type()).isEqualTo(sourceAccount.getType());
            assertThat(sourceAccountTransferResult.balance()).isEqualByComparingTo(sourceAccountBalance);
            assertThat(sourceAccountTransferResult.currency()).isEqualTo(sourceAccountCurrency);
            assertThat(sourceAccountTransferResult.isArchived()).isEqualTo(false);

            AccountDTO destinationAccountTransferResult = transferResult.toAccount();

            assertThat(destinationAccountTransferResult).isNotNull();
            assertThat(destinationAccountTransferResult.id()).isEqualTo(destinationAccount.getId());
            assertThat(destinationAccountTransferResult.name()).isEqualTo(destinationAccount.getName());
            assertThat(destinationAccountTransferResult.color()).isEqualTo(destinationAccount.getColor());
            assertThat(destinationAccountTransferResult.icon()).isEqualTo(destinationAccount.getIcon());
            assertThat(destinationAccountTransferResult.type()).isEqualTo(destinationAccount.getType());
            assertThat(destinationAccountTransferResult.balance()).isEqualByComparingTo(destinationAccountBalance);
            assertThat(destinationAccountTransferResult.currency()).isEqualTo(transferCurrency);
            assertThat(destinationAccountTransferResult.isArchived()).isEqualTo(false);

            var optionalRecurrenceInDatabase = recurrenceRepository.findById(result.id());

            assertThat(optionalRecurrenceInDatabase).isPresent();

            var recurrenceInDatabase = optionalRecurrenceInDatabase.get();

            assertThat(recurrenceInDatabase.getId()).isEqualTo(result.id());
            assertThat(recurrenceInDatabase.getInterval()).isEqualTo(result.interval());
            assertThat(recurrenceInDatabase.getFirstOccurrence()).isEqualTo(result.firstOccurrence());
            assertThat(recurrenceInDatabase.getTransactionType()).isEqualTo(result.transactionType());
            assertThat(recurrenceInDatabase.getRecurrenceType()).isEqualTo(result.recurrenceType());
            assertThat(recurrenceInDatabase.getUser().getId()).isEqualTo(user.getId());

            var optionalTransferInDatabase = transferRepository.findById(transferResult.id());

            assertThat(optionalTransferInDatabase).isPresent();

            var transferInDatabase = optionalTransferInDatabase.get();

            assertThat(transferInDatabase.getId()).isEqualTo(transferResult.id());
            assertThat(transferInDatabase.getTitle()).isEqualTo(transferResult.title());
            assertThat(transferInDatabase.getDescription()).isEqualTo(transferResult.description());
            assertThat(transferInDatabase.getSourceAccount().getId()).isEqualTo(sourceAccount.getId());
            assertThat(transferInDatabase.getDestinationAccount().getId()).isEqualTo(destinationAccount.getId());
            assertThat(transferInDatabase.getBillingDate()).isEqualTo(transferResult.billingDate());
            assertThat(transferInDatabase.getValue().getAmount()).isEqualByComparingTo(transferResult.value());
            assertThat(transferInDatabase.getValue().getCurrency()).isEqualTo(transferResult.currency());
            assertThat(transferInDatabase.isPaid()).isEqualTo(transferResult.paid());
            assertThat(transferInDatabase.getPaid()).isNull();
            assertThat(transferInDatabase.getInstallmentIndex()).isEqualTo(1);
            assertThat(transferInDatabase.getRecurrence().getId()).isEqualTo(result.id());

            var optionalSourceAccountInDatabase = accountRepository.findById(sourceAccount.getId());

            assertThat(optionalSourceAccountInDatabase).isPresent();

            var sourceAccountInDatabase = optionalSourceAccountInDatabase.get();

            assertThat(sourceAccountInDatabase.getId()).isEqualTo(sourceAccount.getId());
            assertThat(sourceAccountInDatabase.getName()).isEqualTo(sourceAccount.getName());
            assertThat(sourceAccountInDatabase.getColor()).isEqualTo(sourceAccount.getColor());
            assertThat(sourceAccountInDatabase.getIcon()).isEqualTo(sourceAccount.getIcon());
            assertThat(sourceAccountInDatabase.getType()).isEqualTo(sourceAccount.getType());
            assertThat(sourceAccountInDatabase.getBalance().getAmount()).isEqualByComparingTo(sourceAccountBalance);
            assertThat(sourceAccountInDatabase.getBalance().getCurrency()).isEqualTo(sourceAccountCurrency);
            assertThat(sourceAccountInDatabase.isArchived()).isEqualTo(false);

            var optionalDestinationAccountInDatabase = accountRepository.findById(destinationAccount.getId());

            assertThat(optionalDestinationAccountInDatabase).isPresent();

            var destinationAccountInDatabase = optionalDestinationAccountInDatabase.get();

            assertThat(destinationAccountInDatabase.getId()).isEqualTo(destinationAccount.getId());
            assertThat(destinationAccountInDatabase.getName()).isEqualTo(destinationAccount.getName());
            assertThat(destinationAccountInDatabase.getColor()).isEqualTo(destinationAccount.getColor());
            assertThat(destinationAccountInDatabase.getIcon()).isEqualTo(destinationAccount.getIcon());
            assertThat(destinationAccountInDatabase.getType()).isEqualTo(destinationAccount.getType());
            assertThat(destinationAccountInDatabase.getBalance().getAmount()).isEqualByComparingTo(destinationAccountBalance);
            assertThat(destinationAccountInDatabase.getBalance().getCurrency()).isEqualTo(transferCurrency);
            assertThat(destinationAccountInDatabase.isArchived()).isEqualTo(false);
        }

        @Test
        @Disabled("Test not implemented yet")
        @DisplayName("should be able to register a transfer of type \"UNIQUE\" that has been paid and source account has a different currency than the transfer currency")
        // TODO: Implement test when currencyprovider is implemented
        void shouldBeAbleToRegisterATransferOfTypeUniqueThatHasBeenPaidAndSourceAccountHasDifferentCurrencyThanTransferCurrency() {
            // Arrange
            // Act
            // Assert
        }

        @Test
        @Disabled("Test not implemented yet")
        @DisplayName("should be able to register a transfer of type \"UNIQUE\" that has been paid and destination account has a different currency than the transfer currency")
        // TODO: Implement test when currencyprovider is implemented
        void shouldBeAbleToRegisterATransferOfTypeUniqueThatHasBeenPaidAndDestinationAccountHasDifferentCurrencyThanTransferCurrency() {
            // Arrange
            // Act
            // Assert
        }

        @Test
        @Disabled("Test not implemented yet")
        @DisplayName("should be able to register a transfer of type \"UNIQUE\" that has been paid and both accounts have a different currency than the transfer currency")
        // TODO: Implement test when currencyprovider is implemented
        void shouldBeAbleToRegisterATransferOfTypeUniqueThatHasBeenPaidAndBothAccountsHaveADifferentCurrencyThanTheTransferCurrency() {
            // Arrange
            // Act
            // Assert
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if the source account does not exist")
        void shouldThrowResourceNotFoundExceptionIfSourceAccountDoesNotExist() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            BigDecimal destinationAccountBalance = BigDecimal.valueOf(0);
            String destinationAccountCurrency = "BRL";
            Money destinationAccountMoney = new Money(destinationAccountBalance, destinationAccountCurrency);
            Account destinationAccount = createAccount();
            destinationAccount.setBalance(destinationAccountMoney);
            destinationAccount.setUser(user);

            user.setAccounts(List.of(destinationAccount));

            accountRepository.save(destinationAccount);


            BigDecimal amountToTransfer = BigDecimal.valueOf(10000);
            String transferCurrency = destinationAccountCurrency;

            LocalDate billingDate = LocalDate.now().plusDays(3);

            RegisterUniqueTransferDTO registerUniqueTransferDTO = new RegisterUniqueTransferDTO(
                    "Test Transfer",
                    "Test Description",
                    amountToTransfer,
                    transferCurrency,
                    randomUUID().toString(),
                    destinationAccount.getId(),
                    billingDate,
                    null
            );

            // Act & Assert
            assertThatThrownBy(() -> sut.registerUniqueTransfer(registerUniqueTransferDTO, user.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Source Account not found.");
        }

        @Test
        @DisplayName("should throw ResourceIsArchivedException if the source account is archived")
        void shouldThrowResourceIsArchivedExceptionIfSourceAccountIsArchived() {
            // Arrange
            User user = createUser();
            userRepository.save(user);


            BigDecimal sourceAccountBalance = BigDecimal.valueOf(50000);
            String sourceAccountCurrency = "BRL";
            Money sourceAccountMoney = new Money(sourceAccountBalance, sourceAccountCurrency);
            Account sourceAccount = createAccount();
            sourceAccount.archive();
            sourceAccount.setBalance(sourceAccountMoney);
            sourceAccount.setUser(user);

            BigDecimal destinationAccountBalance = BigDecimal.valueOf(0);
            String destinationAccountCurrency = "BRL";
            Money destinationAccountMoney = new Money(destinationAccountBalance, destinationAccountCurrency);
            Account destinationAccount = createAccount();
            destinationAccount.setBalance(destinationAccountMoney);
            destinationAccount.setUser(user);

            user.setAccounts(List.of(sourceAccount, destinationAccount));

            accountRepository.saveAll(List.of(sourceAccount, destinationAccount));


            BigDecimal amountToTransfer = BigDecimal.valueOf(10000);
            String transferCurrency = destinationAccountCurrency;

            var today = LocalDate.now();

            LocalDate billingDate = today.plusDays(3);

            RegisterUniqueTransferDTO registerUniqueTransferDTO = new RegisterUniqueTransferDTO(
                    "Test Transfer",
                    "Test Description",
                    amountToTransfer,
                    transferCurrency,
                    sourceAccount.getId(),
                    destinationAccount.getId(),
                    billingDate,
                    null
            );

            // Act
            assertThatThrownBy(() -> sut.registerUniqueTransfer(registerUniqueTransferDTO, user.getId()))
                    .isInstanceOf(ResourceIsArchivedException.class)
                    .hasMessage("Source Account is archived.");

            // Assert
            var optionalSourceAccountInDatabase = accountRepository.findById(sourceAccount.getId());

            assertThat(optionalSourceAccountInDatabase).isPresent();

            var sourceAccountInDatabase = optionalSourceAccountInDatabase.get();

            assertThat(sourceAccountInDatabase.getId()).isEqualTo(sourceAccount.getId());
            assertThat(sourceAccountInDatabase.getName()).isEqualTo(sourceAccount.getName());
            assertThat(sourceAccountInDatabase.getColor()).isEqualTo(sourceAccount.getColor());
            assertThat(sourceAccountInDatabase.getIcon()).isEqualTo(sourceAccount.getIcon());
            assertThat(sourceAccountInDatabase.getType()).isEqualTo(sourceAccount.getType());
            assertThat(sourceAccountInDatabase.getBalance().getAmount()).isEqualByComparingTo(sourceAccountBalance);
            assertThat(sourceAccountInDatabase.getBalance().getCurrency()).isEqualTo(sourceAccountCurrency);
            assertThat(sourceAccountInDatabase.isArchived()).isEqualTo(true);

            var optionalDestinationAccountInDatabase = accountRepository.findById(destinationAccount.getId());

            assertThat(optionalDestinationAccountInDatabase).isPresent();

            var destinationAccountInDatabase = optionalDestinationAccountInDatabase.get();

            assertThat(destinationAccountInDatabase.getId()).isEqualTo(destinationAccount.getId());
            assertThat(destinationAccountInDatabase.getName()).isEqualTo(destinationAccount.getName());
            assertThat(destinationAccountInDatabase.getColor()).isEqualTo(destinationAccount.getColor());
            assertThat(destinationAccountInDatabase.getIcon()).isEqualTo(destinationAccount.getIcon());
            assertThat(destinationAccountInDatabase.getType()).isEqualTo(destinationAccount.getType());
            assertThat(destinationAccountInDatabase.getBalance().getAmount()).isEqualByComparingTo(destinationAccountBalance);
            assertThat(destinationAccountInDatabase.getBalance().getCurrency()).isEqualTo(transferCurrency);
            assertThat(destinationAccountInDatabase.isArchived()).isEqualTo(false);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException if the destination account does not exist")
        void shouldThrowResourceNotFoundExceptionIfDestinationAccountDoesNotExist() {
            // Arrange
            User user = createUser();
            userRepository.save(user);


            BigDecimal sourceAccountBalance = BigDecimal.valueOf(50000);
            String sourceAccountCurrency = "BRL";
            Money sourceAccountMoney = new Money(sourceAccountBalance, sourceAccountCurrency);
            Account sourceAccount = createAccount();
            sourceAccount.setBalance(sourceAccountMoney);
            sourceAccount.setUser(user);

            user.setAccounts(List.of(sourceAccount));

            accountRepository.saveAll(List.of(sourceAccount));


            BigDecimal amountToTransfer = BigDecimal.valueOf(10000);
            String transferCurrency = "BRL";

            var today = LocalDate.now();

            LocalDate billingDate = today.plusDays(3);

            RegisterUniqueTransferDTO registerUniqueTransferDTO = new RegisterUniqueTransferDTO(
                    "Test Transfer",
                    "Test Description",
                    amountToTransfer,
                    transferCurrency,
                    sourceAccount.getId(),
                    randomUUID().toString(),
                    billingDate,
                    null
            );

            // Act & Assert
            assertThatThrownBy(() -> sut.registerUniqueTransfer(registerUniqueTransferDTO, user.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Destination Account not found.");
        }

        @Test
        @DisplayName("should throw ResourceIsArchivedException if the destination account is archived")
        void shouldThrowResourceIsArchivedExceptionIfDestinationAccountIsArchived() {
            // Arrange
            User user = createUser();
            userRepository.save(user);


            BigDecimal sourceAccountBalance = BigDecimal.valueOf(50000);
            String sourceAccountCurrency = "BRL";
            Money sourceAccountMoney = new Money(sourceAccountBalance, sourceAccountCurrency);
            Account sourceAccount = createAccount();
            sourceAccount.setBalance(sourceAccountMoney);
            sourceAccount.setUser(user);

            BigDecimal destinationAccountBalance = BigDecimal.valueOf(0);
            String destinationAccountCurrency = "BRL";
            Money destinationAccountMoney = new Money(destinationAccountBalance, destinationAccountCurrency);
            Account destinationAccount = createAccount();
            destinationAccount.archive();
            destinationAccount.setBalance(destinationAccountMoney);
            destinationAccount.setUser(user);

            user.setAccounts(List.of(sourceAccount, destinationAccount));

            accountRepository.saveAll(List.of(sourceAccount, destinationAccount));


            BigDecimal amountToTransfer = BigDecimal.valueOf(10000);
            String transferCurrency = destinationAccountCurrency;

            var today = LocalDate.now();

            LocalDate billingDate = today.plusDays(3);

            RegisterUniqueTransferDTO registerUniqueTransferDTO = new RegisterUniqueTransferDTO(
                    "Test Transfer",
                    "Test Description",
                    amountToTransfer,
                    transferCurrency,
                    sourceAccount.getId(),
                    destinationAccount.getId(),
                    billingDate,
                    null
            );

            // Act
            assertThatThrownBy(() -> sut.registerUniqueTransfer(registerUniqueTransferDTO, user.getId()))
                    .isInstanceOf(ResourceIsArchivedException.class)
                    .hasMessage("Destination Account is archived.");

            // Assert
            var optionalSourceAccountInDatabase = accountRepository.findById(sourceAccount.getId());

            assertThat(optionalSourceAccountInDatabase).isPresent();

            var sourceAccountInDatabase = optionalSourceAccountInDatabase.get();

            assertThat(sourceAccountInDatabase.getId()).isEqualTo(sourceAccount.getId());
            assertThat(sourceAccountInDatabase.getName()).isEqualTo(sourceAccount.getName());
            assertThat(sourceAccountInDatabase.getColor()).isEqualTo(sourceAccount.getColor());
            assertThat(sourceAccountInDatabase.getIcon()).isEqualTo(sourceAccount.getIcon());
            assertThat(sourceAccountInDatabase.getType()).isEqualTo(sourceAccount.getType());
            assertThat(sourceAccountInDatabase.getBalance().getAmount()).isEqualByComparingTo(sourceAccountBalance);
            assertThat(sourceAccountInDatabase.getBalance().getCurrency()).isEqualTo(sourceAccountCurrency);
            assertThat(sourceAccountInDatabase.isArchived()).isEqualTo(false);

            var optionalDestinationAccountInDatabase = accountRepository.findById(destinationAccount.getId());

            assertThat(optionalDestinationAccountInDatabase).isPresent();

            var destinationAccountInDatabase = optionalDestinationAccountInDatabase.get();

            assertThat(destinationAccountInDatabase.getId()).isEqualTo(destinationAccount.getId());
            assertThat(destinationAccountInDatabase.getName()).isEqualTo(destinationAccount.getName());
            assertThat(destinationAccountInDatabase.getColor()).isEqualTo(destinationAccount.getColor());
            assertThat(destinationAccountInDatabase.getIcon()).isEqualTo(destinationAccount.getIcon());
            assertThat(destinationAccountInDatabase.getType()).isEqualTo(destinationAccount.getType());
            assertThat(destinationAccountInDatabase.getBalance().getAmount()).isEqualByComparingTo(destinationAccountBalance);
            assertThat(destinationAccountInDatabase.getBalance().getCurrency()).isEqualTo(transferCurrency);
            assertThat(destinationAccountInDatabase.isArchived()).isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("getTransfer tests")
    class GetTransferTests {
        @Test
        @Transactional
        @DisplayName("should be able to get transfer")
        void shouldBeAbleToGetTransfer() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account sourceAccount = createAccount();
            sourceAccount.setUser(user);

            Account destinationAccount = createAccount();
            destinationAccount.setUser(user);

            accountRepository.saveAll(List.of(sourceAccount, destinationAccount));

            LocalDate today = LocalDate.now();

            Recurrence recurrence = Recurrence.builder()
                    .firstOccurrence(today)
                    .interval(RecurrenceInterval.MONTHLY)
                    .transactionType(TransactionType.TRANSFER)
                    .recurrenceType(RecurrenceType.REPEATED)
                    .transfers(new ArrayList<>(2))
                    .user(user)
                    .build();

            recurrenceRepository.save(recurrence);

            Transfer transfer1 = Transfer.builder()
                    .title("Transfer 1")
                    .description("Transfer 1 Description")
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(100), "BRL"))
                    .billingDate(today)
                    .paid(today)
                    .installmentIndex(1)
                    .recurrence(recurrence)
                    .build();

            Transfer transfer2 = Transfer.builder()
                    .title("Transfer 2")
                    .description("Transfer 2 Description")
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(100), "BRL"))
                    .billingDate(today.plusMonths(1))
                    .paid(null)
                    .installmentIndex(2)
                    .recurrence(recurrence)
                    .build();

            List<Transfer> transfers = List.of(transfer1, transfer2);

            recurrence.setTransfers(transfers);

            transferRepository.saveAll(transfers);

            // Act
            RecurrenceDTO<TransferDTO> result = sut.getTransfer(transfer1.getId(), user.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(recurrence.getId());
            assertThat(result.interval()).isEqualTo(recurrence.getInterval());
            assertThat(result.firstOccurrence()).isEqualTo(recurrence.getFirstOccurrence());
            assertThat(result.transactionType()).isEqualTo(recurrence.getTransactionType());
            assertThat(result.recurrenceType()).isEqualTo(recurrence.getRecurrenceType());
            assertThat(result.recurrences())
                    .hasSize(1)
                    .extracting(
                            TransferDTO::id,
                            TransferDTO::title,
                            TransferDTO::description,
                            TransferDTO::value,
                            TransferDTO::currency,
                            TransferDTO::billingDate,
                            TransferDTO::paid,
                            TransferDTO::paidDate,
                            TransferDTO::installmentIndex,
                            TransferDTO::installments,
                            TransferDTO::recurrenceId
                    )
                    .contains(tuple(
                            transfer1.getId(),
                            transfer1.getTitle(),
                            transfer1.getDescription(),
                            transfer1.getValue().getAmount(),
                            transfer1.getValue().getCurrency(),
                            transfer1.getBillingDate(),
                            transfer1.isPaid(),
                            transfer1.getPaid(),
                            1,
                            transfers.size(),
                            recurrence.getId()
                    ));

            result.recurrences().forEach(transferDTO -> {
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

                assertThat(transferDTO.fromAccount()).isEqualTo(sourceAccountDTO);
                assertThat(transferDTO.toAccount()).isEqualTo(destinationAccountDTO);
            });
        }

        @Test
        @Transactional
        @DisplayName("should throw ResourceNotFoundException if transfer not found")
        void shouldThrowResourceNotFoundExceptionIfTransferNotFound() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account sourceAccount = createAccount();
            sourceAccount.setUser(user);

            Account destinationAccount = createAccount();
            destinationAccount.setUser(user);

            accountRepository.saveAll(List.of(sourceAccount, destinationAccount));


            LocalDate today = LocalDate.now();

            Recurrence recurrence = Recurrence.builder()
                    .firstOccurrence(today)
                    .interval(RecurrenceInterval.DAILY)
                    .transactionType(TransactionType.TRANSFER)
                    .recurrenceType(RecurrenceType.REPEATED)
                    .transfers(new ArrayList<>(2))
                    .user(user)
                    .build();

            recurrenceRepository.save(recurrence);

            // Act & Assert
            assertThatThrownBy(() -> sut.getTransfer(randomUUID().toString(), user.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Transfer not found.");
        }
    }

    @Nested
    @DisplayName("listTransfers tests")
    class ListTransfersTests {
        @Test
        @DisplayName("should return an empty page if there are no transfers matching the filters")
        void shouldReturnEmptyPageIfNoTransfers() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            LocalDate startDate = LocalDate.now().minusDays(10);
            LocalDate endDate = LocalDate.now().plusDays(10);
            Pageable pageable = PageRequest.of(
                    0,
                    20,
                    Sort.by("billingDate").ascending()
            );
            Status status = Status.ALL;

            // Act
            Page<RecurrenceDTO<TransferDTO>> result = sut.listTransfers(
                    startDate,
                    endDate,
                    pageable,
                    status,
                    user.getId()
            );

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("should return a page of transfers matching the date range and status filters")
        void shouldReturnPageOfTransfersWithValidFilters() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account sourceAccount = createAccount();
            sourceAccount.setUser(user);
            accountRepository.save(sourceAccount);

            Account destinationAccount = createAccount();
            destinationAccount.setUser(user);
            accountRepository.save(destinationAccount);

            LocalDate today = LocalDate.now();

            Recurrence recurrence = Recurrence.builder()
                    .firstOccurrence(today.minusDays(2))
                    .interval(RecurrenceInterval.DAILY)
                    .transactionType(TransactionType.TRANSFER)
                    .recurrenceType(RecurrenceType.REPEATED)
                    .user(user)
                    .transfers(new ArrayList<>())
                    .build();

            recurrenceRepository.save(recurrence);

            // Transfer 1: yesterday (PAID)
            Transfer transfer1 = Transfer.builder()
                    .title("Transfer 1")
                    .billingDate(today.minusDays(1))
                    .paid(today)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.TEN, "BRL"))
                    .recurrence(recurrence)
                    .build();

            // Transfer 2: today (NOT PAID)
            Transfer transfer2 = Transfer.builder()
                    .title("Transfer 2")
                    .billingDate(today)
                    .paid(null)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(20), "BRL"))
                    .recurrence(recurrence)
                    .build();

            // Transfer 3: tomorrow (NOT PAID)
            Transfer transfer3 = Transfer.builder()
                    .title("Transfer 3")
                    .billingDate(today.plusDays(1))
                    .paid(null)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(30), "BRL"))
                    .recurrence(recurrence)
                    .build();

            transferRepository.saveAll(List.of(transfer1, transfer2, transfer3));
            recurrence.setTransfers(List.of(transfer1, transfer2, transfer3));

            LocalDate startDate = today.minusDays(2);
            LocalDate endDate = today.plusDays(2);

            Pageable pageable = PageRequest.of(0, 20, Sort.by("billingDate").ascending());
            Status paidStatus = Status.PAID;

            // Act
            Page<RecurrenceDTO<TransferDTO>> result = sut.listTransfers(
                    startDate,
                    endDate,
                    pageable,
                    paidStatus,
                    user.getId()
            );

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);

            RecurrenceDTO<TransferDTO> onlyRecurrence = result.getContent().getFirst();
            assertThat(onlyRecurrence.recurrences()).hasSize(1);

            TransferDTO returnedTransfer = onlyRecurrence.recurrences().getFirst();
            assertThat(returnedTransfer.id()).isEqualTo(transfer1.getId());
            assertThat(returnedTransfer.paid()).isTrue();
            assertThat(returnedTransfer.paidDate()).isEqualTo(transfer1.getPaid());
            assertThat(returnedTransfer.title()).isEqualTo(transfer1.getTitle());
            assertThat(returnedTransfer.billingDate()).isEqualTo(transfer1.getBillingDate());
        }

        @RepeatedTest(20)
        @DisplayName("should return multiple transfers when using Status.ALL and date range covers all")
        void shouldReturnMultipleTransfersWhenStatusAllAndDateRangeIsWide() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account sourceAccount = createAccount();
            sourceAccount.setUser(user);

            Account destinationAccount = createAccount();
            destinationAccount.setUser(user);

            accountRepository.saveAll(List.of(sourceAccount, destinationAccount));

            var today = LocalDate.now();

            Recurrence recurrence1 = Recurrence.builder()
                    .firstOccurrence(today)
                    .interval(RecurrenceInterval.DAILY)
                    .transactionType(TransactionType.TRANSFER)
                    .recurrenceType(RecurrenceType.REPEATED)
                    .user(user)
                    .transfers(new ArrayList<>())
                    .build();

            Recurrence recurrence2 = Recurrence.builder()
                    .firstOccurrence(today)
                    .interval(RecurrenceInterval.DAILY)
                    .transactionType(TransactionType.TRANSFER)
                    .recurrenceType(RecurrenceType.REPEATED)
                    .user(user)
                    .transfers(new ArrayList<>())
                    .build();

            List<Recurrence> recurrences = List.of(recurrence1, recurrence2);

            recurrenceRepository.saveAll(recurrences);

            // Transfer A
            Transfer transferA = Transfer.builder()
                    .title("Transfer A")
                    .billingDate(today.minusDays(2))
                    .paid(null)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(15), "BRL"))
                    .installmentIndex(1)
                    .recurrence(recurrence1)
                    .build();

            // Transfer B
            Transfer transferB = Transfer.builder()
                    .title("Transfer B")
                    .billingDate(today)
                    .paid(today)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(25), "BRL"))
                    .installmentIndex(2)
                    .recurrence(recurrence1)
                    .build();

            // Transfer C
            Transfer transferC = Transfer.builder()
                    .title("Transfer C")
                    .billingDate(today)
                    .paid(null)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(35), "BRL"))
                    .installmentIndex(3)
                    .recurrence(recurrence1)
                    .build();

            // Transfer D
            Transfer transferD = Transfer.builder()
                    .title("Transfer D")
                    .billingDate(today)
                    .paid(null)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(15), "BRL"))
                    .installmentIndex(1)
                    .recurrence(recurrence2)
                    .build();

            // Transfer E
            Transfer transferE = Transfer.builder()
                    .title("Transfer E")
                    .billingDate(today)
                    .paid(null)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(65), "BRL"))
                    .installmentIndex(2)
                    .recurrence(recurrence2)
                    .build();

            Transfer transferF = Transfer.builder()
                    .title("Transfer F")
                    .billingDate(today.plusMonths(2))
                    .paid(null)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(5), "BRL"))
                    .installmentIndex(3)
                    .recurrence(recurrence2)
                    .build();

            List<Transfer> transfers1 = List.of(transferA, transferB, transferC);
            List<Transfer> transfers2 = List.of(transferD, transferE, transferF);

            List<Transfer> transfers = List.of(transferA, transferB, transferC, transferD, transferE, transferF);

            transferRepository.saveAll(transfers);
            recurrence1.setTransfers(transfers1);
            recurrence2.setTransfers(transfers2);

            // We set a wide range that captures all transfers
            LocalDate startDate = today.minusDays(3);
            LocalDate endDate = today.plusDays(3);
            Pageable pageable = PageRequest.of(0, 10, Sort.by("billingDate").ascending());
            Status status = Status.ALL;

            // Act
            Page<RecurrenceDTO<TransferDTO>> result = sut.listTransfers(
                    startDate,
                    endDate,
                    pageable,
                    status,
                    user.getId()
            );

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(5);

            var content = result.getContent();

            assertThat(content).hasSize(5);

            var recurrenceTransfer1 = content.getFirst();

            assertThat(recurrenceTransfer1.id()).isEqualTo(recurrence1.getId());
            assertThat(recurrenceTransfer1.interval()).isEqualTo(recurrence1.getInterval());
            assertThat(recurrenceTransfer1.firstOccurrence()).isEqualTo(recurrence1.getFirstOccurrence());
            assertThat(recurrenceTransfer1.transactionType()).isEqualTo(recurrence1.getTransactionType());
            assertThat(recurrenceTransfer1.recurrenceType()).isEqualTo(recurrence1.getRecurrenceType());
            assertThat(recurrenceTransfer1.recurrences())
                    .hasSize(1)
                    .extracting(
                            TransferDTO::id,
                            TransferDTO::title,
                            TransferDTO::description,
                            TransferDTO::billingDate,
                            TransferDTO::paid,
                            TransferDTO::paidDate,
                            TransferDTO::installmentIndex,
                            TransferDTO::installments,
                            TransferDTO::recurrenceId
                    ).contains(tuple(
                            transferA.getId(),
                            transferA.getTitle(),
                            transferA.getDescription(),
                            transferA.getBillingDate(),
                            transferA.isPaid(),
                            transferA.getPaid(),
                            1,
                            transfers1.size(),
                            transferA.getRecurrence().getId()
                    ));

            var recurrenceTransfer2 = content.get(1);

            assertThat(recurrenceTransfer2.id()).isEqualTo(recurrence1.getId());
            assertThat(recurrenceTransfer2.interval()).isEqualTo(recurrence1.getInterval());
            assertThat(recurrenceTransfer2.firstOccurrence()).isEqualTo(recurrence1.getFirstOccurrence());
            assertThat(recurrenceTransfer2.transactionType()).isEqualTo(recurrence1.getTransactionType());
            assertThat(recurrenceTransfer2.recurrenceType()).isEqualTo(recurrence1.getRecurrenceType());
            assertThat(recurrenceTransfer2.recurrences())
                    .hasSize(1)
                    .extracting(
                            TransferDTO::id,
                            TransferDTO::title,
                            TransferDTO::description,
                            TransferDTO::billingDate,
                            TransferDTO::paid,
                            TransferDTO::paidDate,
                            TransferDTO::installmentIndex,
                            TransferDTO::installments,
                            TransferDTO::recurrenceId
                    ).contains(tuple(
                            transferB.getId(),
                            transferB.getTitle(),
                            transferB.getDescription(),
                            transferB.getBillingDate(),
                            transferB.isPaid(),
                            transferB.getPaid(),
                            2,
                            transfers1.size(),
                            transferB.getRecurrence().getId()
                    ));


            var recurrenceTransfer3 = content.get(2);

            assertThat(recurrenceTransfer3.id()).isEqualTo(recurrence1.getId());
            assertThat(recurrenceTransfer3.interval()).isEqualTo(recurrence1.getInterval());
            assertThat(recurrenceTransfer3.firstOccurrence()).isEqualTo(recurrence1.getFirstOccurrence());
            assertThat(recurrenceTransfer3.transactionType()).isEqualTo(recurrence1.getTransactionType());
            assertThat(recurrenceTransfer3.recurrenceType()).isEqualTo(recurrence1.getRecurrenceType());
            assertThat(recurrenceTransfer3.recurrences())
                    .hasSize(1)
                    .extracting(
                            TransferDTO::id,
                            TransferDTO::title,
                            TransferDTO::description,
                            TransferDTO::billingDate,
                            TransferDTO::paid,
                            TransferDTO::paidDate,
                            TransferDTO::installmentIndex,
                            TransferDTO::installments,
                            TransferDTO::recurrenceId
                    ).contains(tuple(
                            transferC.getId(),
                            transferC.getTitle(),
                            transferC.getDescription(),
                            transferC.getBillingDate(),
                            transferC.isPaid(),
                            transferC.getPaid(),
                            3,
                            transfers1.size(),
                            transferC.getRecurrence().getId()
                    ));

            var recurrenceTransfer4 = content.get(3);

            assertThat(recurrenceTransfer4.id()).isEqualTo(recurrence2.getId());
            assertThat(recurrenceTransfer4.interval()).isEqualTo(recurrence2.getInterval());
            assertThat(recurrenceTransfer4.firstOccurrence()).isEqualTo(recurrence2.getFirstOccurrence());
            assertThat(recurrenceTransfer4.transactionType()).isEqualTo(recurrence2.getTransactionType());
            assertThat(recurrenceTransfer4.recurrenceType()).isEqualTo(recurrence2.getRecurrenceType());
            assertThat(recurrenceTransfer4.recurrences())
                    .hasSize(1)
                    .extracting(
                            TransferDTO::id,
                            TransferDTO::title,
                            TransferDTO::description,
                            TransferDTO::billingDate,
                            TransferDTO::paid,
                            TransferDTO::paidDate,
                            TransferDTO::installmentIndex,
                            TransferDTO::installments,
                            TransferDTO::recurrenceId
                    ).contains(tuple(
                            transferD.getId(),
                            transferD.getTitle(),
                            transferD.getDescription(),
                            transferD.getBillingDate(),
                            transferD.isPaid(),
                            transferD.getPaid(),
                            1,
                            transfers2.size(),
                            transferD.getRecurrence().getId()
                    ));

            var recurrenceTransfer5 = content.get(4);

            assertThat(recurrenceTransfer5.id()).isEqualTo(recurrence2.getId());
            assertThat(recurrenceTransfer5.interval()).isEqualTo(recurrence2.getInterval());
            assertThat(recurrenceTransfer5.firstOccurrence()).isEqualTo(recurrence2.getFirstOccurrence());
            assertThat(recurrenceTransfer5.transactionType()).isEqualTo(recurrence2.getTransactionType());
            assertThat(recurrenceTransfer5.recurrenceType()).isEqualTo(recurrence2.getRecurrenceType());
            assertThat(recurrenceTransfer5.recurrences())
                    .hasSize(1)
                    .extracting(
                            TransferDTO::id,
                            TransferDTO::title,
                            TransferDTO::description,
                            TransferDTO::billingDate,
                            TransferDTO::paid,
                            TransferDTO::paidDate,
                            TransferDTO::installmentIndex,
                            TransferDTO::installments,
                            TransferDTO::recurrenceId
                    ).contains(tuple(
                            transferE.getId(),
                            transferE.getTitle(),
                            transferE.getDescription(),
                            transferE.getBillingDate(),
                            transferE.isPaid(),
                            transferE.getPaid(),
                            2,
                            transfers2.size(),
                            transferE.getRecurrence().getId()
                    ));
        }
    }

    @Nested
    @DisplayName("payTransfer tests")
    class PayTransferTests {

        @Test
        @Transactional
        @DisplayName("should pay an existing transfer successfully when no custom source account is provided")
        void shouldPayExistingTransferSuccessfullyWithoutCustomSourceAccount() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account sourceAccount = createAccount();
            sourceAccount.setBalance(new Money(BigDecimal.valueOf(1000), "BRL"));
            sourceAccount.setUser(user);

            Account destinationAccount = createAccount();
            destinationAccount.setBalance(new Money(BigDecimal.valueOf(0), "BRL"));
            destinationAccount.setUser(user);

            List<Account> accounts = List.of(sourceAccount, destinationAccount);
            accountRepository.saveAll(accounts);

            var today = LocalDate.now();

            Recurrence recurrence = Recurrence.builder()
                    .firstOccurrence(today)
                    .interval(RecurrenceInterval.MONTHLY)
                    .transactionType(TransactionType.TRANSFER)
                    .recurrenceType(RecurrenceType.UNIQUE)
                    .transfers(new ArrayList<>())
                    .user(user)
                    .build();

            recurrenceRepository.save(recurrence);

            Transfer transfer = Transfer.builder()
                    .title("Payable Transfer")
                    .description("Transfer to be paid")
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(1000), "BRL"))
                    .billingDate(today)
                    .paid(null)
                    .recurrence(recurrence)
                    .build();

            recurrence.setTransfers(List.of(transfer));
            transferRepository.save(transfer);

            PayTransferDTO payTransferDTO = new PayTransferDTO(null, today);

            // Act
            RecurrenceDTO<TransferDTO> result = sut.payTransfer(transfer.getId(), payTransferDTO, user.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(recurrence.getId());
            assertThat(result.interval()).isEqualTo(recurrence.getInterval());
            assertThat(result.firstOccurrence()).isEqualTo(recurrence.getFirstOccurrence());
            assertThat(result.transactionType()).isEqualTo(recurrence.getTransactionType());
            assertThat(result.recurrenceType()).isEqualTo(recurrence.getRecurrenceType());
            assertThat(result.recurrences()).hasSize(1);

            TransferDTO resultTransfer = result.recurrences().getFirst();

            assertThat(resultTransfer.id()).isEqualTo(transfer.getId());
            assertThat(resultTransfer.title()).isEqualTo(transfer.getTitle());
            assertThat(resultTransfer.description()).isEqualTo(transfer.getDescription());
            assertThat(resultTransfer.billingDate()).isEqualTo(transfer.getBillingDate());
            assertThat(resultTransfer.value()).isEqualTo(transfer.getValue().getAmount());
            assertThat(resultTransfer.currency()).isEqualTo(transfer.getValue().getCurrency());
            assertThat(resultTransfer.installmentIndex()).isEqualTo(transfer.getInstallmentIndex());
            assertThat(resultTransfer.installments()).isEqualTo(1);
            assertThat(resultTransfer.recurrenceId()).isEqualTo(recurrence.getId());
            assertThat(resultTransfer.paid()).isTrue();
            assertThat(resultTransfer.paidDate()).isEqualTo(transfer.getPaid());
            assertThat(resultTransfer.fromAccount().id()).isEqualTo(sourceAccount.getId());
            assertThat(resultTransfer.toAccount().id()).isEqualTo(destinationAccount.getId());
            assertThat(resultTransfer.fromAccount().balance()).isEqualByComparingTo(BigDecimal.valueOf(0));
            assertThat(resultTransfer.toAccount().balance()).isEqualByComparingTo(BigDecimal.valueOf(1000));

            var sourceAccountInDatabase = accountRepository.findById(sourceAccount.getId()).orElseThrow();

            assertThat(sourceAccountInDatabase.getBalance().getAmount()).isEqualTo(BigDecimal.valueOf(0));

            var destinationAccountInDatabase = accountRepository.findById(destinationAccount.getId()).orElseThrow();

            assertThat(destinationAccountInDatabase.getBalance().getAmount()).isEqualTo(BigDecimal.valueOf(1000));
        }

        @Test
        @Transactional
        @DisplayName("should pay an existing transfer successfully when a different source account is provided")
        void shouldPayExistingTransferWithDifferentSourceAccount() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account sourceAccount = createAccount();
            sourceAccount.setBalance(new Money(BigDecimal.valueOf(1000), "BRL"));
            sourceAccount.setUser(user);

            Account differentSourceAccount = createAccount();
            differentSourceAccount.setBalance(new Money(BigDecimal.valueOf(1000), "BRL"));
            differentSourceAccount.setUser(user);

            Account destinationAccount = createAccount();
            destinationAccount.setBalance(new Money(BigDecimal.valueOf(0), "BRL"));
            destinationAccount.setUser(user);

            List<Account> accounts = List.of(sourceAccount, differentSourceAccount, destinationAccount);

            accountRepository.saveAll(accounts);

            var today = LocalDate.now();

            Recurrence recurrence = Recurrence.builder()
                    .firstOccurrence(today)
                    .interval(RecurrenceInterval.MONTHLY)
                    .transactionType(TransactionType.TRANSFER)
                    .recurrenceType(RecurrenceType.UNIQUE)
                    .transfers(new ArrayList<>())
                    .user(user)
                    .build();

            recurrenceRepository.save(recurrence);

            Transfer transfer = Transfer.builder()
                    .title("Payable Transfer with Different Source Account")
                    .description("Transfer to be paid from a different account")
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(1000), "BRL"))
                    .billingDate(today)
                    .paid(null)
                    .recurrence(recurrence)
                    .build();

            recurrence.setTransfers(List.of(transfer));
            transferRepository.save(transfer);

            PayTransferDTO payTransferDTO = new PayTransferDTO(differentSourceAccount.getId(), today);

            // Act
            RecurrenceDTO<TransferDTO> result = sut.payTransfer(transfer.getId(), payTransferDTO, user.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(recurrence.getId());
            assertThat(result.interval()).isEqualTo(recurrence.getInterval());
            assertThat(result.firstOccurrence()).isEqualTo(recurrence.getFirstOccurrence());
            assertThat(result.transactionType()).isEqualTo(recurrence.getTransactionType());
            assertThat(result.recurrenceType()).isEqualTo(recurrence.getRecurrenceType());
            assertThat(result.recurrences()).hasSize(1);

            TransferDTO resultTransfer = result.recurrences().getFirst();

            assertThat(resultTransfer.id()).isEqualTo(transfer.getId());
            assertThat(resultTransfer.title()).isEqualTo(transfer.getTitle());
            assertThat(resultTransfer.description()).isEqualTo(transfer.getDescription());
            assertThat(resultTransfer.billingDate()).isEqualTo(transfer.getBillingDate());
            assertThat(resultTransfer.value()).isEqualTo(transfer.getValue().getAmount());
            assertThat(resultTransfer.currency()).isEqualTo(transfer.getValue().getCurrency());
            assertThat(resultTransfer.installmentIndex()).isEqualTo(transfer.getInstallmentIndex());
            assertThat(resultTransfer.installments()).isEqualTo(1);
            assertThat(resultTransfer.recurrenceId()).isEqualTo(recurrence.getId());
            assertThat(resultTransfer.paid()).isTrue();
            assertThat(resultTransfer.paidDate()).isEqualTo(transfer.getPaid());
            assertThat(resultTransfer.fromAccount().id()).isEqualTo(differentSourceAccount.getId());
            assertThat(resultTransfer.toAccount().id()).isEqualTo(destinationAccount.getId());
            assertThat(resultTransfer.fromAccount().balance()).isEqualByComparingTo(BigDecimal.valueOf(0));
            assertThat(resultTransfer.toAccount().balance()).isEqualByComparingTo(BigDecimal.valueOf(1000));

            var sourceAccountInDatabase = accountRepository.findById(sourceAccount.getId()).orElseThrow();

            assertThat(sourceAccountInDatabase.getBalance().getAmount()).isEqualTo(BigDecimal.valueOf(1000));

            var differentSourceAccountInDatabase = accountRepository.findById(differentSourceAccount.getId()).orElseThrow();

            assertThat(differentSourceAccountInDatabase.getBalance().getAmount()).isEqualTo(BigDecimal.valueOf(0));

            var destinationAccountInDatabase = accountRepository.findById(destinationAccount.getId()).orElseThrow();

            assertThat(destinationAccountInDatabase.getBalance().getAmount()).isEqualTo(BigDecimal.valueOf(1000));
        }

        @Test
        @Transactional
        @DisplayName("should throw ResourceNotFoundException if the transfer does not exist")
        void shouldThrowResourceNotFoundExceptionIfTransferDoesNotExist() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            PayTransferDTO payTransferDTO = new PayTransferDTO(null, LocalDate.now());

            // Act & Assert
            assertThatThrownBy(() -> sut.payTransfer(randomUUID().toString(), payTransferDTO, user.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Transfer not found.");
        }

        @Test
        @Transactional
        @DisplayName("should throw TransferAlreadyPaidException if the transfer is already paid")
        void shouldThrowTransferAlreadyPaidExceptionIfTransferIsAlreadyPaid() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account sourceAccount = createAccount();
            sourceAccount.setUser(user);

            Account destinationAccount = createAccount();
            destinationAccount.setUser(user);

            List<Account> accounts = List.of(sourceAccount, destinationAccount);

            accountRepository.saveAll(accounts);

            Recurrence recurrence = Recurrence.builder()
                    .firstOccurrence(LocalDate.now())
                    .interval(RecurrenceInterval.MONTHLY)
                    .transactionType(TransactionType.TRANSFER)
                    .recurrenceType(RecurrenceType.UNIQUE)
                    .user(user)
                    .build();


            recurrenceRepository.save(recurrence);

            var today = LocalDate.now();

            Transfer transfer = Transfer.builder()
                    .title("Already Paid Transfer")
                    .description("This transfer is already paid")
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.TEN, "BRL"))
                    .billingDate(today)
                    .paid(today)
                    .recurrence(recurrence)
                    .build();

            recurrence.setTransfers(List.of(transfer));
            transferRepository.save(transfer);

            PayTransferDTO payTransferDTO  = new PayTransferDTO(null, today);

            // Act & Assert
            assertThatThrownBy(() -> sut.payTransfer(transfer.getId(), payTransferDTO, user.getId()))
                    .isInstanceOf(TransferAlreadyPaidException.class);
        }

        @Test
        @Transactional
        @DisplayName("should throw ResourceNotFoundException if a custom source accountId does not exist in the database")
        void shouldThrowResourceNotFoundExceptionIfCustomAccountIdNotFound() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account destinationAccount = createAccount();
            destinationAccount.setUser(user);

            accountRepository.save(destinationAccount);

            var today = LocalDate.now();

            Recurrence recurrence = Recurrence.builder()
                    .firstOccurrence(today)
                    .interval(RecurrenceInterval.MONTHLY)
                    .transactionType(TransactionType.TRANSFER)
                    .recurrenceType(RecurrenceType.UNIQUE)
                    .user(user)
                    .build();

            recurrenceRepository.save(recurrence);

            Transfer transfer = Transfer.builder()
                    .title("Transfer with non-existent custom account")
                    .description("Trying to pay using an invalid accountId")
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(500), "BRL"))
                    .billingDate(LocalDate.now())
                    .paid(null)
                    .recurrence(recurrence)
                    .build();

            recurrence.setTransfers(List.of(transfer));
            transferRepository.save(transfer);

            PayTransferDTO payTransferDTO = new PayTransferDTO(randomUUID().toString(), today);

            // Act & Assert
            assertThatThrownBy(() -> sut.payTransfer(transfer.getId(), payTransferDTO, user.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Account not found.");
        }

        @Test
        @Transactional
        @DisplayName("should throw ResourceIsArchivedException if the source account (after choosing or default) is archived")
        void shouldThrowResourceIsArchivedExceptionIfSourceAccountIsArchived() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account archivedSourceAccount = createAccount();
            archivedSourceAccount.setUser(user);
            archivedSourceAccount.archive();
            accountRepository.save(archivedSourceAccount);

            Account destinationAccount = createAccount();
            destinationAccount.setUser(user);
            accountRepository.save(destinationAccount);

            List<Account> accounts = List.of(archivedSourceAccount, destinationAccount);
            accountRepository.saveAll(accounts);

            var today = LocalDate.now();

            Recurrence recurrence = Recurrence.builder()
                    .firstOccurrence(today)
                    .interval(RecurrenceInterval.MONTHLY)
                    .transactionType(TransactionType.TRANSFER)
                    .recurrenceType(RecurrenceType.UNIQUE)
                    .user(user)
                    .build();

            recurrenceRepository.save(recurrence);

            Transfer transfer = Transfer.builder()
                    .title("Transfer to pay from archived source")
                    .description("Source account is archived, cannot pay")
                    .sourceAccount(archivedSourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(100), "BRL"))
                    .billingDate(today)
                    .paid(null)
                    .recurrence(recurrence)
                    .build();

            recurrence.setTransfers(List.of(transfer));
            transferRepository.save(transfer);

            PayTransferDTO payTransferDTO = new PayTransferDTO(null, today);

            // Act & Assert
            assertThatThrownBy(() -> sut.payTransfer(transfer.getId(), payTransferDTO, user.getId()))
                    .isInstanceOf(ResourceIsArchivedException.class)
                    .hasMessage("Unable to pay transfer if source account is archived.");
        }

        @Test
        @Transactional
        @DisplayName("should throw InvalidTransferDestinationException if the destination account is null")
        void shouldThrowInvalidTransferDestinationExceptionIfDestinationAccountIsNull() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account sourceAccount = createAccount();
            sourceAccount.setUser(user);
            accountRepository.save(sourceAccount);

            var today = LocalDate.now();

            Recurrence recurrence = Recurrence.builder()
                    .firstOccurrence(today)
                    .interval(RecurrenceInterval.MONTHLY)
                    .transactionType(TransactionType.TRANSFER)
                    .recurrenceType(RecurrenceType.UNIQUE)
                    .user(user)
                    .build();

            recurrenceRepository.save(recurrence);

            Transfer transfer = Transfer.builder()
                    .title("Transfer with null destination")
                    .description("Invalid transfer because destination is null")
                    .sourceAccount(sourceAccount)
                    .value(new Money(BigDecimal.valueOf(100), "BRL"))
                    .billingDate(today)
                    .paid(null)
                    .recurrence(recurrence)
                    .build();

            recurrence.setTransfers(List.of(transfer));
            transferRepository.save(transfer);

            PayTransferDTO payTransferDTO = new PayTransferDTO(null, today);

            // Act & Assert
            assertThatThrownBy(() -> sut.payTransfer(transfer.getId(), payTransferDTO, user.getId()))
                    .isInstanceOf(InvalidTransferDestinationException.class);
        }
    }

    @Nested
    @DisplayName("unpayTransfer tests")
    class UnpayTransferTests {
        @Test
        @Transactional
        @DisplayName("should be able to unpay a transfer that was previously paid")
        void shouldUnpayPreviouslyPaidTransfer() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account sourceAccount = createAccount();
            sourceAccount.setUser(user);
            sourceAccount.setBalance(new Money(BigDecimal.valueOf(0), "BRL"));

            Account destinationAccount = createAccount();
            destinationAccount.setUser(user);
            destinationAccount.setBalance(new Money(BigDecimal.valueOf(1000), "BRL"));

            accountRepository.saveAll(List.of(sourceAccount, destinationAccount));

            var today = LocalDate.now();

            Recurrence recurrence = Recurrence.builder()
                    .recurrenceType(RecurrenceType.REPEATED)
                    .firstOccurrence(today)
                    .interval(RecurrenceInterval.MONTHLY)
                    .transactionType(TransactionType.TRANSFER)
                    .user(user)
                    .build();

            recurrenceRepository.save(recurrence);

            Transfer paidTransfer = Transfer.builder()
                    .title("Paid Transfer")
                    .description("Transfer that is already paid")
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(1000), "BRL"))
                    .billingDate(today)
                    .paid(today)
                    .recurrence(recurrence)
                    .build();

            recurrence.setTransfers(List.of(paidTransfer));
            transferRepository.save(paidTransfer);

            // Act
            RecurrenceDTO<TransferDTO> result = sut.unpayTransfer(paidTransfer.getId(), user.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(recurrence.getId());
            assertThat(result.interval()).isEqualTo(recurrence.getInterval());
            assertThat(result.firstOccurrence()).isEqualTo(recurrence.getFirstOccurrence());
            assertThat(result.transactionType()).isEqualTo(recurrence.getTransactionType());
            assertThat(result.recurrenceType()).isEqualTo(recurrence.getRecurrenceType());
            assertThat(result.recurrences()).hasSize(1);

            TransferDTO unpayResult = result.recurrences().getFirst();

            assertThat(unpayResult.id()).isEqualTo(paidTransfer.getId());
            assertThat(unpayResult.title()).isEqualTo(paidTransfer.getTitle());
            assertThat(unpayResult.description()).isEqualTo(paidTransfer.getDescription());
            assertThat(unpayResult.value()).isEqualByComparingTo(paidTransfer.getValue().getAmount());
            assertThat(unpayResult.currency()).isEqualTo(paidTransfer.getValue().getCurrency());
            assertThat(unpayResult.billingDate()).isEqualTo(paidTransfer.getBillingDate());
            assertThat(unpayResult.description()).isEqualTo(paidTransfer.getDescription());
            assertThat(unpayResult.paid()).isFalse();
            assertThat(unpayResult.paidDate()).isNull();

            assertThat(unpayResult.fromAccount().balance()).isEqualByComparingTo("1000");
            assertThat(unpayResult.toAccount().balance()).isEqualByComparingTo("0");

            Account updatedSource = accountRepository.findById(sourceAccount.getId()).orElseThrow();
            Account updatedDestination = accountRepository.findById(destinationAccount.getId()).orElseThrow();

            assertThat(updatedSource.getBalance().getAmount()).isEqualByComparingTo("1000");
            assertThat(updatedDestination.getBalance().getAmount()).isEqualByComparingTo("0");
        }

        @Test
        @Transactional
        @DisplayName("should throw ResourceNotFoundException if transfer does not exist")
        void shouldThrowResourceNotFoundExceptionIfTransferNotFound() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            // Act & Assert
            assertThatThrownBy(() -> sut.unpayTransfer(randomUUID().toString(), user.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Transfer not found.");
        }

        @Test
        @DisplayName("should throw TransferNotPaidYetException if transfer is not paid")
        void shouldThrowTransferNotPaidYetExceptionIfTransferIsNotPaid() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account sourceAccount = createAccount();
            sourceAccount.setUser(user);
            sourceAccount.setBalance(new Money(BigDecimal.valueOf(1000), "BRL"));

            Account destinationAccount = createAccount();
            destinationAccount.setUser(user);
            destinationAccount.setBalance(new Money(BigDecimal.valueOf(0), "BRL"));

            accountRepository.saveAll(List.of(sourceAccount, destinationAccount));

            var today = LocalDate.now();

            Recurrence recurrence = Recurrence.builder()
                    .recurrenceType(RecurrenceType.UNIQUE)
                    .firstOccurrence(today)
                    .interval(RecurrenceInterval.MONTHLY)
                    .transactionType(TransactionType.TRANSFER)
                    .user(user)
                    .build();

            recurrenceRepository.save(recurrence);

            Transfer unpaidTransfer = Transfer.builder()
                    .paid(null)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(sourceAccount)
                    .billingDate(today)
                    .value(new Money(BigDecimal.valueOf(1000), "BRL"))
                    .recurrence(recurrence)
                    .title("Unpaid Transfer")
                    .build();

            recurrence.setTransfers(List.of(unpaidTransfer));

            transferRepository.save(unpaidTransfer);

            // Act & Assert
            assertThatThrownBy(() -> sut.unpayTransfer(unpaidTransfer.getId(), user.getId()))
                    .isInstanceOf(TransferNotPaidYetException.class);
        }

        @Test
        @Transactional
        @DisplayName("should throw InvalidTransferSourceException if source account is null")
        void shouldThrowInvalidTransferSourceExceptionIfSourceNull() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account destinationAccount = createAccount();
            destinationAccount.setUser(user);
            accountRepository.save(destinationAccount);

            var today = LocalDate.now();

            Recurrence recurrence = Recurrence.builder()
                    .recurrenceType(RecurrenceType.UNIQUE)
                    .firstOccurrence(today)
                    .interval(RecurrenceInterval.MONTHLY)
                    .transactionType(TransactionType.TRANSFER)
                    .user(user)
                    .build();

            recurrenceRepository.save(recurrence);

            Transfer paidTransfer = Transfer.builder()
                    .title("Paid Transfer without Source")
                    .description("Source account is missing")
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(100), "BRL"))
                    .billingDate(today)
                    .paid(today)
                    .recurrence(recurrence)
                    .build();

            recurrence.setTransfers(List.of(paidTransfer));
            transferRepository.save(paidTransfer);

            // Act & Assert
            assertThatThrownBy(() -> sut.unpayTransfer(paidTransfer.getId(), user.getId()))
                    .isInstanceOf(InvalidTransferSourceException.class)
                    .hasMessage("Transfer Source Account not found.");
        }

        @Test
        @Transactional
        @DisplayName("should throw InvalidTransferDestinationException if destination account is null")
        void shouldThrowInvalidTransferDestinationExceptionIfDestinationNull() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account sourceAccount = createAccount();
            sourceAccount.setUser(user);
            accountRepository.save(sourceAccount);

            var today = LocalDate.now();

            Recurrence recurrence = Recurrence.builder()
                    .recurrenceType(RecurrenceType.UNIQUE)
                    .firstOccurrence(today)
                    .interval(RecurrenceInterval.MONTHLY)
                    .transactionType(TransactionType.TRANSFER)
                    .user(user)
                    .build();

            recurrenceRepository.save(recurrence);

            Transfer paidTransfer = Transfer.builder()
                    .title("Paid Transfer without Destination")
                    .description("Destination account is missing")
                    .sourceAccount(sourceAccount)
                    .value(new Money(BigDecimal.valueOf(100), "BRL"))
                    .billingDate(today)
                    .paid(today)
                    .recurrence(recurrence)
                    .build();

            recurrence.setTransfers(List.of(paidTransfer));
            transferRepository.save(paidTransfer);

            // Act & Assert
            assertThatThrownBy(() -> sut.unpayTransfer(paidTransfer.getId(), user.getId()))
                    .isInstanceOf(InvalidTransferDestinationException.class)
                    .hasMessage("Transfer Destination Account not found.");
        }
    }

    @Nested
    @DisplayName("deleteTransfer tests")
    class DeleteTransferTests {
        @Test
        @Transactional
        @DisplayName("should be able to delete a transfer")
        void shouldBeAbleToDeleteTransfer() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account sourceAccount = createAccount();
            sourceAccount.setBalance(new Money(BigDecimal.valueOf(1000), "BRL"));
            sourceAccount.setUser(user);

            Account destinationAccount = createAccount();
            destinationAccount.setBalance(new Money(BigDecimal.valueOf(0), "BRL"));
            destinationAccount.setUser(user);

            List<Account> accounts = List.of(sourceAccount, destinationAccount);
            accountRepository.saveAll(accounts);

            var today = LocalDate.now();

            Recurrence recurrence = Recurrence.builder()
                    .recurrenceType(RecurrenceType.REPEATED)
                    .firstOccurrence(today)
                    .interval(RecurrenceInterval.MONTHLY)
                    .transactionType(TransactionType.TRANSFER)
                    .user(user)
                    .build();

            recurrenceRepository.save(recurrence);

            Transfer transferA = Transfer.builder()
                    .title("Transfer A")
                    .billingDate(today.plusMonths(1))
                    .paid(null)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(15), "BRL"))
                    .installmentIndex(1)
                    .recurrence(recurrence)
                    .build();

            Transfer transferB = Transfer.builder()
                    .title("Transfer B")
                    .billingDate(today.plusMonths(2))
                    .paid(today)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(25), "BRL"))
                    .installmentIndex(2)
                    .recurrence(recurrence)
                    .build();

            Transfer transferC = Transfer.builder()
                    .title("Transfer C")
                    .billingDate(today.plusMonths(3))
                    .paid(null)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(35), "BRL"))
                    .installmentIndex(3)
                    .recurrence(recurrence)
                    .build();

            Transfer transferD = Transfer.builder()
                    .title("Transfer D")
                    .billingDate(today.plusMonths(4))
                    .paid(null)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(45), "BRL"))
                    .installmentIndex(4)
                    .recurrence(recurrence)
                    .build();

            List<Transfer> transfers = List.of(transferA, transferB, transferC, transferD);

            recurrence.setTransfers(transfers);
            transferRepository.saveAll(transfers);

            // Act
            sut.deleteTransfer(transferC.getId(), user.getId());

            // Assert
            assertThat(transferRepository.findById(transferC.getId())).isEmpty();
        }

        @Test
        @Transactional
        @DisplayName("should throw ResourceNotFoundException if transfer not found")
        void shouldThrowResourceNotFoundExceptionIfTransferNotFound() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            // Act & Assert
            assertThatThrownBy(() -> sut.deleteTransfer(randomUUID().toString(), user.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Transfer not found.");
        }
    }

    @Nested
    @DisplayName("deleteFutureTransfers tests")
    class DeleteFutureTransfersTests {
        @Test
        @Transactional
        @DisplayName("should be able to delete future transfers")
        void shouldBeAbleToDeleteFutureTransfers() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account sourceAccount = createAccount();
            sourceAccount.setBalance(new Money(BigDecimal.valueOf(1000), "BRL"));
            sourceAccount.setUser(user);

            Account destinationAccount = createAccount();
            destinationAccount.setBalance(new Money(BigDecimal.valueOf(0), "BRL"));
            destinationAccount.setUser(user);

            List<Account> accounts = List.of(sourceAccount, destinationAccount);
            accountRepository.saveAll(accounts);

            var today = LocalDate.now();

            Recurrence recurrence = Recurrence.builder()
                    .recurrenceType(RecurrenceType.REPEATED)
                    .firstOccurrence(today)
                    .interval(RecurrenceInterval.MONTHLY)
                    .transactionType(TransactionType.TRANSFER)
                    .user(user)
                    .build();

            recurrenceRepository.save(recurrence);

            Transfer transferA = Transfer.builder()
                    .title("Transfer A")
                    .billingDate(today.plusMonths(1))
                    .paid(null)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(15), "BRL"))
                    .installmentIndex(1)
                    .recurrence(recurrence)
                    .build();

            Transfer transferB = Transfer.builder()
                    .title("Transfer B")
                    .billingDate(today.plusMonths(2))
                    .paid(today)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(25), "BRL"))
                    .installmentIndex(2)
                    .recurrence(recurrence)
                    .build();

            Transfer transferC = Transfer.builder()
                    .title("Transfer C")
                    .billingDate(today.plusMonths(3))
                    .paid(null)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(35), "BRL"))
                    .installmentIndex(3)
                    .recurrence(recurrence)
                    .build();

            Transfer transferD = Transfer.builder()
                    .title("Transfer D")
                    .billingDate(today.plusMonths(4))
                    .paid(null)
                    .sourceAccount(sourceAccount)
                    .destinationAccount(destinationAccount)
                    .value(new Money(BigDecimal.valueOf(45), "BRL"))
                    .installmentIndex(4)
                    .recurrence(recurrence)
                    .build();

            List<Transfer> transfers = List.of(transferA, transferB, transferC, transferD);

            recurrence.setTransfers(transfers);
            transferRepository.saveAll(transfers);

            // Act
            sut.deleteFutureTransfers(recurrence.getId(), 3, user.getId());

            // Assert
            assertThat(transferRepository.findById(transferA.getId())).isPresent();
            assertThat(transferRepository.findById(transferB.getId())).isPresent();
            assertThat(transferRepository.findById(transferC.getId())).isEmpty();
            assertThat(transferRepository.findById(transferD.getId())).isEmpty();
        }

        @Test
        @Transactional
        @DisplayName("should throw ResourceNotFoundException if Recurrence not found")
        void shouldThrowResourceNotFoundExceptionIfRecurrenceNotFound() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            // Act & Assert
            assertThatThrownBy(() -> sut.deleteFutureTransfers(randomUUID().toString(), 3, user.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Recurrence not found.");
        }
    }
}