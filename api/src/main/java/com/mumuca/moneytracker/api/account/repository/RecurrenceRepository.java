package com.mumuca.moneytracker.api.account.repository;

import com.mumuca.moneytracker.api.account.model.Recurrence;
import com.mumuca.moneytracker.api.account.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecurrenceRepository extends JpaRepository<Recurrence, String> {
    @Query("""
        SELECT r FROM Recurrence r
        JOIN FETCH r.transfers t
        WHERE t.id = :transferId AND r.user.id = :userId
    """)
    Optional<Recurrence> findByTransferIdAndUserId(
            @Param("transferId") String transferId,
            @Param("userId") String userId
    );
}
