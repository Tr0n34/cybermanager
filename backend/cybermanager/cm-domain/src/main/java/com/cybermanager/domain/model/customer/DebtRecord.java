package com.cybermanager.domain.model.customer;

import com.cybermanager.domain.model.shared.Money;

import java.time.LocalDateTime;
import java.util.Objects;

public record DebtRecord(
        DebtId id,
        CustomerId customerId,
        String label,
        Money amount,
        DebtStatus status,
        LocalDateTime createdAt,
        LocalDateTime settledAt
) {
    public DebtRecord {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(customerId, "customerId is required");
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Debt label is required");
        }
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
    }

    public static DebtRecord create(CustomerId customerId, String label, Money amount, LocalDateTime createdAt) {
        return new DebtRecord(DebtId.newId(), customerId, label.trim(), amount, DebtStatus.OPEN, createdAt, null);
    }

    public DebtRecord settle(LocalDateTime settledAt) {
        return new DebtRecord(id, customerId, label, amount, DebtStatus.SETTLED, createdAt, settledAt);
    }
}
