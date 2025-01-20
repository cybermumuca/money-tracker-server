package com.mumuca.moneytracker.api.account.repository.specification;

import com.mumuca.moneytracker.api.account.model.Recurrence;
import com.mumuca.moneytracker.api.account.model.Status;
import com.mumuca.moneytracker.api.account.model.Transfer;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransferSpecification {
    public static Specification<Transfer> withFilters(
            String userId,
            LocalDate startDate,
            LocalDate endDate,
            Status status
    ) {
        return (transfer, query, cb) -> {
            Join<Transfer, Recurrence> recurrence = transfer.join("recurrence");

            List<Predicate> predicates = new ArrayList<>();

            // Filter by user id
            predicates.add(belongsToUser(recurrence, cb, userId));

            // Filter by date range
            predicates.add(billingDateBetween(transfer, cb, startDate, endDate));

            if (status != Status.ALL) {
                switch (status) {
                    case PAID -> predicates.add(cb.isTrue(transfer.get("paid")));
                    case OVERDUE -> {
                        // Exemplo: COBRA transfers vencidas (data < hoje e não pagas)
                        predicates.add(cb.lessThan(transfer.get("billingDate"), LocalDate.now()));
                        predicates.add(cb.isFalse(transfer.get("paid")));
                    }
                    case PENDING -> {
                        // Exemplo: data >= hoje e não pagas
                        predicates.add(cb.greaterThanOrEqualTo(transfer.get("billingDate"), LocalDate.now()));
                        predicates.add(cb.isFalse(transfer.get("paid")));
                    }
                    case IGNORED -> {
                        // TODO: Implement ignored
                    }
                    default -> {}
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate belongsToUser(
            Join<Transfer, Recurrence> recurrence,
            CriteriaBuilder cb,
            String userId
    ) {
        return cb.equal(recurrence.get("user").get("id"), userId);
    }

    private static Predicate billingDateBetween(
            Root<Transfer> transfer,
            CriteriaBuilder cb,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return cb.between(transfer.get("billingDate"), startDate, endDate);
    }
}
