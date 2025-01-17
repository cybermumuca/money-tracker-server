package com.mumuca.moneytracker.api.account.repository;

import com.mumuca.moneytracker.api.account.model.Recurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecurrenceRepository extends JpaRepository<Recurrence, String> {}
