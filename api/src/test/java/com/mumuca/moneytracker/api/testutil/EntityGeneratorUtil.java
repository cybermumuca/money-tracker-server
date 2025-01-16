package com.mumuca.moneytracker.api.testutil;

import com.github.javafaker.Faker;
import com.mumuca.moneytracker.api.account.model.Account;
import com.mumuca.moneytracker.api.account.model.AccountType;
import com.mumuca.moneytracker.api.auth.model.Gender;
import com.mumuca.moneytracker.api.auth.model.User;
import com.mumuca.moneytracker.api.model.Money;

import java.math.BigDecimal;

import static java.time.ZoneId.systemDefault;

public class EntityGeneratorUtil {
    public static final Faker faker = new Faker();

    public static User createUser() {
        return User.builder()
                .firstName(faker.name().firstName())
                .lastName(faker.name().lastName())
                .email(faker.internet().emailAddress())
                .password(faker.internet().password())
                .gender(faker.options().option(Gender.class))
                .birthDate(faker.date().birthday().toInstant().atZone(systemDefault()).toLocalDate())
                .photoUrl(faker.internet().avatar())
                .build();
    }

    public static Account createAccount() {
        return Account.builder()
                .name(faker.lorem().word())
                .color(faker.color().name())
                .icon(faker.internet().avatar())
                .type(faker.options().option(AccountType.class))
                .money(new Money(BigDecimal.valueOf(faker.number().numberBetween(0, 700)), faker.currency().code()))
                .build();
    }
}
