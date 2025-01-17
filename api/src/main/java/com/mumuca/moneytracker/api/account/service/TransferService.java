package com.mumuca.moneytracker.api.account.service;

import com.mumuca.moneytracker.api.account.dto.RecurrenceDTO;
import com.mumuca.moneytracker.api.account.dto.RegisterRepeatedTransferDTO;
import com.mumuca.moneytracker.api.account.dto.RegisterUniqueTransferDTO;
import com.mumuca.moneytracker.api.account.dto.TransferDTO;
import jakarta.validation.Valid;

public interface TransferService {
    RecurrenceDTO<TransferDTO> registerUniqueTransfer(
            RegisterUniqueTransferDTO registerUniqueTransferDTO,
            String userId
    );

    RecurrenceDTO<TransferDTO> registerRepeatedTransfer(
            RegisterRepeatedTransferDTO registerRepeatedTransferDTO,
            String userId
    );
}
