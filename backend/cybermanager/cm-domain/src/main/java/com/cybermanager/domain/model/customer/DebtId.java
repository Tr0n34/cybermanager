package com.cybermanager.domain.model.customer;

import java.util.Objects;
import java.util.UUID;

public record DebtId(UUID value) {
    public DebtId {
        Objects.requireNonNull(value, "value is required");
    }

    public static DebtId newId() {
        return new DebtId(UUID.randomUUID());
    }
}
