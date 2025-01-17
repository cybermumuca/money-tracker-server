package com.mumuca.moneytracker.api.account.repository;

import com.mumuca.moneytracker.api.account.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, String> {

}
