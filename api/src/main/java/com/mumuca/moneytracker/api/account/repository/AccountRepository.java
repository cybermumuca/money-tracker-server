package com.mumuca.moneytracker.api.account.repository;

import com.mumuca.moneytracker.api.account.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByIdAndUserId(String id, String userId);

    @Query("SELECT a FROM Account a WHERE a.user.id = :userId AND a.isArchived = false")
    List<Account> findActiveAccountsByUserId(@Param("userId") String userId);

    @Query("SELECT a FROM Account a WHERE a.user.id = :userId AND a.isArchived = true")
    List<Account> findArchivedAccountsByUserId(@Param("userId") String userId);
}
