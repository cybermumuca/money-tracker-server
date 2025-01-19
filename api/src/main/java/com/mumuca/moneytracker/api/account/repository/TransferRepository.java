package com.mumuca.moneytracker.api.account.repository;

import com.mumuca.moneytracker.api.account.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, String> {
    @Query("""
        SELECT t FROM Transfer t
        WHERE t.recurrence.id = :recurrenceId
        ORDER BY t.billingDate
    """)
    List<Transfer> findTransfersByRecurrenceId(@Param("recurrenceId") String recurrenceId);
}
