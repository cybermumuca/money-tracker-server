package com.mumuca.moneytracker.api.account.service.impl;

import com.mumuca.moneytracker.api.account.dto.AccountDTO;
import com.mumuca.moneytracker.api.account.dto.CreateAccountDTO;
import com.mumuca.moneytracker.api.account.dto.EditAccountDTO;
import com.mumuca.moneytracker.api.account.dto.WithdrawDTO;
import com.mumuca.moneytracker.api.account.model.Account;
import com.mumuca.moneytracker.api.account.repository.AccountRepository;
import com.mumuca.moneytracker.api.account.service.AccountService;
import com.mumuca.moneytracker.api.auth.model.User;
import com.mumuca.moneytracker.api.exception.ResourceNotFoundException;
import com.mumuca.moneytracker.api.model.Money;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    public AccountDTO createAccount(CreateAccountDTO createAccountDTO, String userId) {
        Account account = Account.builder()
                .name(createAccountDTO.name())
                .color(createAccountDTO.color())
                .icon(createAccountDTO.icon())
                .type(createAccountDTO.type())
                .money(new Money(createAccountDTO.balance(), createAccountDTO.currency()))
                .user(new User(userId))
                .build();

        accountRepository.save(account);

        return new AccountDTO(
                account.getId(),
                account.getName(),
                account.getColor(),
                account.getIcon(),
                account.getType(),
                account.getMoney().getBalance(),
                account.getMoney().getCurrency(),
                account.isArchived()
        );
    }

    @Override
    public AccountDTO getAccount(String accountId, String userId) {
        Account account = accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        return new AccountDTO(
                account.getId(),
                account.getName(),
                account.getColor(),
                account.getIcon(),
                account.getType(),
                account.getMoney().getBalance(),
                account.getMoney().getCurrency(),
                account.isArchived()
        );
    }

    @Override
    public List<AccountDTO> listActiveAccounts(String userId) {
        return accountRepository
                .findActiveAccountsByUserId(userId)
                .stream()
                .map(account -> new AccountDTO(
                        account.getId(),
                        account.getName(),
                        account.getColor(),
                        account.getIcon(),
                        account.getType(),
                        account.getMoney().getBalance(),
                        account.getMoney().getCurrency(),
                        account.isArchived()
                ))
                .toList();
    }

    @Override
    public List<AccountDTO> listArchivedAccounts(String userId) {
        return accountRepository
                .findArchivedAccountsByUserId(userId)
                .stream()
                .map(account -> new AccountDTO(
                        account.getId(),
                        account.getName(),
                        account.getColor(),
                        account.getIcon(),
                        account.getType(),
                        account.getMoney().getBalance(),
                        account.getMoney().getCurrency(),
                        account.isArchived()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void archiveAccount(String accountId, String userId) {
        Account accountToArchive = accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        accountToArchive.archive();

        accountRepository.save(accountToArchive);
    }

    @Override
    @Transactional
    public void unarchiveAccount(String accountId, String userId) {
        Account accountToActive = accountRepository
                .findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        accountToActive.unarchive();

        accountRepository.save(accountToActive);
    }

    @Override
    @Transactional
    public void deleteAccount(String accountId, String userId) {
        Account accountToDelete = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        accountRepository.deleteById(accountToDelete.getId());
    }

    @Override
    @Transactional
    public AccountDTO editAccount(String accountId, EditAccountDTO editAccountDTO, String userId) {
        Account accountToEdit = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));

        accountToEdit.setName(editAccountDTO.name());
        accountToEdit.setColor(editAccountDTO.color());
        accountToEdit.setIcon(editAccountDTO.icon());
        accountToEdit.setType(editAccountDTO.type());
        accountToEdit.getMoney().setBalance(editAccountDTO.balance());
        accountToEdit.getMoney().setCurrency(editAccountDTO.currency());

        accountRepository.save(accountToEdit);

        return new AccountDTO(
                accountToEdit.getId(),
                accountToEdit.getName(),
                accountToEdit.getColor(),
                accountToEdit.getIcon(),
                accountToEdit.getType(),
                accountToEdit.getMoney().getBalance(),
                accountToEdit.getMoney().getCurrency(),
                accountToEdit.isArchived()
        );
    }

    @Override
    @Transactional
    // TODO: Implement withdraw when Expense is implemented
    public AccountDTO withdraw(String accountId, WithdrawDTO withdrawDTO, String userId) {
//        Account accountToWithdraw = accountRepository.findByIdAndUserId(userId, accountId)
//                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
//
//        accountToWithdraw.getMoney().withdraw(withdrawDTO.amount());
//
//        accountRepository.save(accountToWithdraw);
//
//        return new AccountDTO(
//                accountToWithdraw.getId(),
//                accountToWithdraw.getName(),
//                accountToWithdraw.getColor(),
//                accountToWithdraw.getIcon(),
//                accountToWithdraw.getType(),
//                accountToWithdraw.getMoney().getBalance(),
//                accountToWithdraw.getMoney().getCurrency(),
//                accountToWithdraw.isArchived()
//        );
        return null;
    }
}
