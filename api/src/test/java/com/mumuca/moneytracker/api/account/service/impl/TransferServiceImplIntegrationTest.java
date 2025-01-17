package com.mumuca.moneytracker.api.account.service.impl;

import com.mumuca.moneytracker.api.account.dto.AccountDTO;
import com.mumuca.moneytracker.api.account.dto.RecurrenceDTO;
import com.mumuca.moneytracker.api.account.dto.RegisterUniqueTransferDTO;
import com.mumuca.moneytracker.api.account.dto.TransferDTO;
import com.mumuca.moneytracker.api.account.model.Account;
import com.mumuca.moneytracker.api.account.model.RecurrenceInterval;
import com.mumuca.moneytracker.api.account.model.RecurrenceType;
import com.mumuca.moneytracker.api.account.model.TransactionType;
import com.mumuca.moneytracker.api.account.repository.AccountRepository;
import com.mumuca.moneytracker.api.account.repository.RecurrenceRepository;
import com.mumuca.moneytracker.api.account.repository.TransferRepository;
import com.mumuca.moneytracker.api.auth.model.User;
import com.mumuca.moneytracker.api.auth.repository.UserRepository;
import com.mumuca.moneytracker.api.exception.ResourceIsArchivedException;
import com.mumuca.moneytracker.api.exception.ResourceNotFoundException;
import com.mumuca.moneytracker.api.model.Money;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


import static com.mumuca.moneytracker.api.testutil.EntityGeneratorUtil.createAccount;
import static com.mumuca.moneytracker.api.testutil.EntityGeneratorUtil.createUser;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

            LocalDate billingDate = LocalDate.now().plusDays(3);

            RegisterUniqueTransferDTO registerUniqueTransferDTO = new RegisterUniqueTransferDTO(
                    "Test Transfer",
                    "Test Description",
                    amountToTransfer,
                    transferCurrency,
                    sourceAccount.getId(),
                    destinationAccount.getId(),
                    billingDate,
                    true
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

            LocalDate billingDate = LocalDate.now().plusDays(3);

            RegisterUniqueTransferDTO registerUniqueTransferDTO = new RegisterUniqueTransferDTO(
                    "Test Transfer",
                    "Test Description",
                    amountToTransfer,
                    transferCurrency,
                    sourceAccount.getId(),
                    destinationAccount.getId(),
                    billingDate,
                    false
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
            assertThat(transferResult.paid()).isFalse();
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
                    true
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

            LocalDate billingDate = LocalDate.now().plusDays(3);

            RegisterUniqueTransferDTO registerUniqueTransferDTO = new RegisterUniqueTransferDTO(
                    "Test Transfer",
                    "Test Description",
                    amountToTransfer,
                    transferCurrency,
                    sourceAccount.getId(),
                    destinationAccount.getId(),
                    billingDate,
                    true
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

            LocalDate billingDate = LocalDate.now().plusDays(3);

            RegisterUniqueTransferDTO registerUniqueTransferDTO = new RegisterUniqueTransferDTO(
                    "Test Transfer",
                    "Test Description",
                    amountToTransfer,
                    transferCurrency,
                    sourceAccount.getId(),
                    randomUUID().toString(),
                    billingDate,
                    true
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

            LocalDate billingDate = LocalDate.now().plusDays(3);

            RegisterUniqueTransferDTO registerUniqueTransferDTO = new RegisterUniqueTransferDTO(
                    "Test Transfer",
                    "Test Description",
                    amountToTransfer,
                    transferCurrency,
                    sourceAccount.getId(),
                    destinationAccount.getId(),
                    billingDate,
                    true
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
}