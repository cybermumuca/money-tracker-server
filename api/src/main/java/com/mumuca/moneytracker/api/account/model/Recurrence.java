package com.mumuca.moneytracker.api.account.model;

import com.mumuca.moneytracker.api.auth.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "recurrences")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Recurrence {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_interval")
    @JdbcType(value = PostgreSQLEnumJdbcType.class)
    private RecurrenceInterval interval;

    @Column(name = "first_occurrence")
    private LocalDate firstOccurrence;

    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type")
    @JdbcType(value = PostgreSQLEnumJdbcType.class)
    private RecurrenceType recurrenceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    @JdbcType(value = PostgreSQLEnumJdbcType.class)
    private TransactionType transactionType;

    @OneToMany(mappedBy = "recurrence", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transfer> transfers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
