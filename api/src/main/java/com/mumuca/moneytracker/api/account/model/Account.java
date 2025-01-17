package com.mumuca.moneytracker.api.account.model;

import com.mumuca.moneytracker.api.auth.model.User;
import com.mumuca.moneytracker.api.model.Archivable;
import com.mumuca.moneytracker.api.model.Money;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Account extends Archivable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "color")
    private String color;

    @Column(name = "icon")
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    @JdbcType(value = PostgreSQLEnumJdbcType.class)
    private AccountType type;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "balance", column = @Column(name = "balance")),
            @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money balance;

    @OneToMany(mappedBy = "sourceAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transfer> sentTransfers = new ArrayList<>();

    @OneToMany(mappedBy = "destinationAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transfer> receivedTransfers = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;


    @Override
    public void archive() {
        super.archive();
    }

    @Override
    public void unarchive() {
        super.unarchive();
    }

    public void deposit(Money money) {
        this.balance.add(money);
    }

    public void deposit(BigDecimal amount) {
        this.balance.add(amount);
    }

    public void withdraw(Money money) {
        this.balance.subtract(money);
    }

    public void withdraw(BigDecimal amount) {
        this.balance.subtract(amount);
    }
}
