package com.mumuca.moneytracker.api.account.service.impl;

import com.mumuca.moneytracker.api.account.dto.AccountDTO;
import com.mumuca.moneytracker.api.account.dto.CreateAccountDTO;
import com.mumuca.moneytracker.api.account.dto.EditAccountDTO;
import com.mumuca.moneytracker.api.account.model.Account;
import com.mumuca.moneytracker.api.account.model.AccountType;
import com.mumuca.moneytracker.api.account.repository.AccountRepository;
import com.mumuca.moneytracker.api.auth.model.User;
import com.mumuca.moneytracker.api.auth.repository.UserRepository;
import com.mumuca.moneytracker.api.exception.ResourceAlreadyActiveException;
import com.mumuca.moneytracker.api.exception.ResourceAlreadyArchivedException;
import com.mumuca.moneytracker.api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.mumuca.moneytracker.api.testutil.EntityGeneratorUtil.createAccount;
import static com.mumuca.moneytracker.api.testutil.EntityGeneratorUtil.createUser;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("AccountServiceImpl Integration Tests")
class AccountServiceImplIntegrationTest {

    @Autowired
    private AccountServiceImpl sut;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("createAccount tests")
    class CreateAccountTests {
        @Test
        @Transactional
        @DisplayName("should be able to create an account")
        void shouldBeAbleToCreateAccount() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            CreateAccountDTO createAccountDTO = new CreateAccountDTO(
                    AccountType.WALLET,
                    "Account Name",
                    BigDecimal.valueOf(1000),
                    "USD",
                    "icon",
                    "#FFFFFF"
            );

            // Act
            AccountDTO result = sut.createAccount(createAccountDTO, user.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isNotNull();
            assertThat(result.name()).isEqualTo(createAccountDTO.name());
            assertThat(result.color()).isEqualTo(createAccountDTO.color());
            assertThat(result.icon()).isEqualTo(createAccountDTO.icon());
            assertThat(result.type()).isEqualTo(createAccountDTO.type());
            assertThat(result.balance()).isEqualByComparingTo(createAccountDTO.balance());
            assertThat(result.currency()).isEqualTo(createAccountDTO.currency());
            assertThat(result.isArchived()).isFalse();

            var accountInDatabase = accountRepository.findById(result.id()).orElseThrow();

            assertThat(accountInDatabase).isNotNull();
            assertThat(accountInDatabase.getName()).isEqualTo(createAccountDTO.name());
            assertThat(accountInDatabase.getColor()).isEqualTo(createAccountDTO.color());
            assertThat(accountInDatabase.getIcon()).isEqualTo(createAccountDTO.icon());
            assertThat(accountInDatabase.getType()).isEqualTo(createAccountDTO.type());
            assertThat(accountInDatabase.getMoney().getBalance()).isEqualByComparingTo(createAccountDTO.balance());
            assertThat(accountInDatabase.getMoney().getCurrency()).isEqualTo(createAccountDTO.currency());
            assertThat(accountInDatabase.isArchived()).isFalse();
            assertThat(accountInDatabase.getUser().getId()).isEqualTo(user.getId());
        }
    }

    @Nested
    @DisplayName("getAccount tests")
    class GetAccountTests {
        @Test
        @Transactional
        @DisplayName("should be able to get an account")
        void shouldBeAbleToRetrieveAccount() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account account = createAccount();
            account.setUser(user);
            accountRepository.save(account);

            // Act
            AccountDTO result = sut.getAccount(account.getId(), user.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(account.getId());
            assertThat(result.name()).isEqualTo(account.getName());
            assertThat(result.color()).isEqualTo(account.getColor());
            assertThat(result.icon()).isEqualTo(account.getIcon());
            assertThat(result.type()).isEqualTo(account.getType());
            assertThat(result.balance()).isEqualByComparingTo(account.getMoney().getBalance());
            assertThat(result.currency()).isEqualTo(account.getMoney().getCurrency());
            assertThat(result.isArchived()).isEqualTo(account.isArchived());
        }

        @Test
        @Transactional
        @DisplayName("should throw an ResourceNotFoundException when account does not exist")
        void shouldThrowAnResourceNotFoundExceptionWhenAccountDoesNotExist() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            // Act & Assert
            assertThatThrownBy(() -> sut.getAccount(randomUUID().toString(), user.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found.");
        }
    }

    @Nested
    @DisplayName("listActiveAccounts tests")
    class ListActiveAccountsTests {
        @Test
        @Transactional
        @DisplayName("should be able to list only active accounts")
        void shouldBeAbleToListOnlyActiveAccounts() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account activeAccount = createAccount();
            activeAccount.setUser(user);

            Account archivedAccount = createAccount();
            archivedAccount.setUser(user);
            archivedAccount.archive();

            accountRepository.saveAll(List.of(activeAccount, archivedAccount));

            // Act
            List<AccountDTO> result = sut.listActiveAccounts(user.getId());

            // Assert
            assertThat(result).hasSize(1);

            var accountResult = result.getFirst();
            assertThat(accountResult.id()).isEqualTo(activeAccount.getId());
            assertThat(accountResult.name()).isEqualTo(activeAccount.getName());
            assertThat(accountResult.color()).isEqualTo(activeAccount.getColor());
            assertThat(accountResult.icon()).isEqualTo(activeAccount.getIcon());
            assertThat(accountResult.type()).isEqualTo(activeAccount.getType());
            assertThat(accountResult.balance()).isEqualByComparingTo(activeAccount.getMoney().getBalance());
            assertThat(accountResult.currency()).isEqualTo(activeAccount.getMoney().getCurrency());
            assertThat(accountResult.isArchived()).isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("listArchivedAccounts tests")
    class ListArchivedAccountsTests {
        @Test
        @Transactional
        @DisplayName("should be able to list only archived accounts")
        void shouldListOnlyArchivedAccounts() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account activeAccount = createAccount();
            activeAccount.setUser(user);

            Account archivedAccount = createAccount();
            archivedAccount.setUser(user);
            archivedAccount.archive();
            accountRepository.saveAll(List.of(activeAccount, archivedAccount));

            // Act
            List<AccountDTO> result = sut.listArchivedAccounts(user.getId());

            // Assert
            assertThat(result).hasSize(1);

            var accountResult = result.getFirst();
            assertThat(accountResult.id()).isEqualTo(archivedAccount.getId());
            assertThat(accountResult.name()).isEqualTo(archivedAccount.getName());
            assertThat(accountResult.color()).isEqualTo(archivedAccount.getColor());
            assertThat(accountResult.icon()).isEqualTo(archivedAccount.getIcon());
            assertThat(accountResult.type()).isEqualTo(archivedAccount.getType());
            assertThat(accountResult.balance()).isEqualByComparingTo(archivedAccount.getMoney().getBalance());
            assertThat(accountResult.currency()).isEqualTo(archivedAccount.getMoney().getCurrency());
            assertThat(accountResult.isArchived()).isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("archiveAccount tests")
    class ArchiveAccountTests {
        @Test
        @Transactional
        @DisplayName("should be able to archive an account")
        void shouldBeAbleToArchiveAccount() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account account = createAccount();
            account.setUser(user);
            accountRepository.save(account);

            // Act
            sut.archiveAccount(account.getId(), user.getId());

            // Assert
            var archivedAccount = accountRepository.findById(account.getId()).orElseThrow();
            assertThat(archivedAccount.isArchived()).isTrue();
        }

        @Test
        @Transactional
        @DisplayName("should throw an ResourceNotFoundException when account does not exist")
        void shouldThrowAnResourceNotFoundExceptionWhenAccountDoesNotExist() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            // Act & Assert
            assertThatThrownBy(() -> sut.archiveAccount(randomUUID().toString(), user.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found.");
        }

        @Test
        @Transactional
        @DisplayName("should throw an ResourceAlreadyArchivedException when account is already archived")
        void shouldThrowAnResourceAlreadyArchivedExceptionWhenAccountIsAlreadyArchived() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account account = createAccount();
            account.setUser(user);
            account.archive();
            accountRepository.save(account);

            // Act & Assert
            assertThatThrownBy(() -> sut.archiveAccount(account.getId(), user.getId()))
                    .isInstanceOf(ResourceAlreadyArchivedException.class);
        }
    }

    @Nested
    @DisplayName("unarchiveAccount tests")
    class UnarchiveAccountTests {
        @Test
        @Transactional
        @DisplayName("should unarchive an account")
        void shouldUnarchiveAccount() {
            User user = createUser();
            userRepository.save(user);

            Account account = createAccount();
            account.setUser(user);
            account.archive();
            accountRepository.save(account);

            sut.unarchiveAccount(account.getId(), user.getId());

            var unarchivedAccount = accountRepository.findById(account.getId()).orElseThrow();
            assertThat(unarchivedAccount.isArchived()).isFalse();
        }

        @Test
        @Transactional
        @DisplayName("should throw an ResourceNotFoundException when account does not exist")
        void shouldThrowAnResourceNotFoundExceptionWhenAccountDoesNotExist() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            // Act & Assert
            assertThatThrownBy(() -> sut.unarchiveAccount(randomUUID().toString(), user.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found.");
        }

        @Test
        @Transactional
        @DisplayName("should throw an ResourceAlreadyActiveException when account is already archived")
        void shouldThrowAnResourceAlreadyActiveExceptionWhenAccountIsAlreadyArchived() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account account = createAccount();
            account.setUser(user);
            accountRepository.save(account);

            // Act & Assert
            assertThatThrownBy(() -> sut.unarchiveAccount(account.getId(), user.getId()))
                    .isInstanceOf(ResourceAlreadyActiveException.class);
        }
    }

    @Nested
    @DisplayName("deleteAccount tests")
    class DeleteAccountTests {
        @Test
        @Transactional
        @DisplayName("should be able to delete an account")
        void shouldBeAbleToDeleteAccount() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account account = createAccount();
            account.setUser(user);
            accountRepository.save(account);

            // Act
            sut.deleteAccount(account.getId(), user.getId());

            // Assert
            assertThat(accountRepository.findById(account.getId())).isEmpty();
        }

        @Test
        @Transactional
        @DisplayName("should throw ResourceNotFoundException when deleting a non-existing account")
        void shouldThrowResourceNotFoundExceptionWhenDeletingNonExistingAccount() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            // Act & Assert
            assertThatThrownBy(() -> sut.deleteAccount(randomUUID().toString(), user.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Account not found.");
        }
    }

    @Nested
    @DisplayName("editAccount tests")
    class EditAccountTests {
        @Test
        @Transactional
        @DisplayName("should be able to edit an account")
        void shouldBeAbleToEditAccount() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            Account account = createAccount();
            account.setUser(user);
            accountRepository.save(account);

            EditAccountDTO editAccountDTO = new EditAccountDTO(
                    AccountType.CHECKING_ACCOUNT,
                    "New Name",
                    BigDecimal.valueOf(500),
                    "EUR",
                    "new-icon",
                    "#000000"
            );

            // Act
            AccountDTO result = sut.editAccount(account.getId(), editAccountDTO, user.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(editAccountDTO.name());
            assertThat(result.color()).isEqualTo(editAccountDTO.color());
            assertThat(result.icon()).isEqualTo(editAccountDTO.icon());
            assertThat(result.type()).isEqualTo(editAccountDTO.type());
            assertThat(result.balance()).isEqualByComparingTo(editAccountDTO.balance());
            assertThat(result.currency()).isEqualTo(editAccountDTO.currency());
        }

        @Test
        @Transactional
        @DisplayName("should throw ResourceNotFoundException when editing a non-existing account")
        void shouldThrowResourceNotFoundExceptionWhenEditingNonExistingAccount() {
            // Arrange
            User user = createUser();
            userRepository.save(user);

            EditAccountDTO editAccountDTO = new EditAccountDTO(
                    AccountType.CHECKING_ACCOUNT,
                    "New Name",
                    BigDecimal.valueOf(500),
                    "EUR",
                    "new-icon",
                    "#000000"
            );
            // Act & Assert
            assertThatThrownBy(() -> sut.editAccount(randomUUID().toString(), editAccountDTO, user.getId()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Account not found.");
        }
    }

}