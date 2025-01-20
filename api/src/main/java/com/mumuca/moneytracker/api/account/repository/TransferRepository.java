package com.mumuca.moneytracker.api.account.repository;

import com.mumuca.moneytracker.api.account.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, String>, JpaSpecificationExecutor<Transfer> {
    @Query("""
        SELECT t FROM Transfer t
        WHERE t.recurrence.id = :recurrenceId
        ORDER BY t.billingDate, t.createdDate
    """)
    List<Transfer> findTransfersByRecurrenceId(@Param("recurrenceId") String recurrenceId);

    @Query("""
        SELECT COUNT(t) FROM Transfer t
        WHERE t.recurrence.id = :recurrenceId
    """)
    int countTransfersByRecurrenceId(@Param("recurrenceId") String recurrenceId);

    @Query("""
        SELECT t FROM Transfer t
        WHERE t.id = :transferId AND t.recurrence.user.id = :userId
    """)
    Optional<Transfer> findTransferById(@Param("transferId") String transferId, @Param("userId") String userId);
}
