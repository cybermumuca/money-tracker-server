package com.mumuca.moneytracker.api.account.repository;

import com.mumuca.moneytracker.api.account.model.Recurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecurrenceRepository extends JpaRepository<Recurrence, String> {
    @Query("""
        SELECT r FROM Recurrence r
        JOIN FETCH r.transfers t
        WHERE r.id = :recurrenceId AND r.user.id = :userId
    """)
    Optional<Recurrence> findByIdAndUserIdWithTransfers(
            @Param("recurrenceId") String recurrenceId,
            @Param("userId") String userId
    );
}
