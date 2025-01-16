package com.mumuca.moneytracker.api.account.service;

import com.mumuca.moneytracker.api.account.dto.AccountDTO;
import com.mumuca.moneytracker.api.account.dto.CreateAccountDTO;
import com.mumuca.moneytracker.api.account.dto.EditAccountDTO;
import com.mumuca.moneytracker.api.account.dto.WithdrawDTO;
import jakarta.validation.Valid;

import java.util.List;

public interface AccountService {
    AccountDTO createAccount(CreateAccountDTO createAccountDTO, String userId);

    AccountDTO getAccount(String accountId, String userId);

    List<AccountDTO> listActiveAccounts(String userId);

    List<AccountDTO> listArchivedAccounts(String userId);

    void archiveAccount(String accountId, String userId);

    void unarchiveAccount(String accountId, String userId);

    void deleteAccount(String accountId, String userId);

    AccountDTO editAccount(String accountId, EditAccountDTO editAccountDTO, String userId);

    AccountDTO withdraw(String accountId, WithdrawDTO withdrawDTO, String userId);
}
