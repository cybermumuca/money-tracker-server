package com.mumuca.moneytracker.api.account.service;

import com.mumuca.moneytracker.api.account.dto.RecurrenceDTO;
import com.mumuca.moneytracker.api.account.dto.RegisterRepeatedTransferDTO;
import com.mumuca.moneytracker.api.account.dto.RegisterUniqueTransferDTO;
import com.mumuca.moneytracker.api.account.dto.TransferDTO;
import com.mumuca.moneytracker.api.account.model.Status;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface TransferService {
    RecurrenceDTO<TransferDTO> registerUniqueTransfer(
            RegisterUniqueTransferDTO registerUniqueTransferDTO,
            String userId
    );

    RecurrenceDTO<TransferDTO> registerRepeatedTransfer(
            RegisterRepeatedTransferDTO registerRepeatedTransferDTO,
            String userId
    );

    RecurrenceDTO<TransferDTO> getTransfer(String transferId, String userId);

    Page<RecurrenceDTO<TransferDTO>> listTransfers(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable,
            Status status,
            String userId
    );

    RecurrenceDTO<TransferDTO> payTransfer(String transferId, String accountId, String userId);
}
