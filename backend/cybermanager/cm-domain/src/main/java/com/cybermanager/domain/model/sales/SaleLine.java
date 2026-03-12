package com.cybermanager.domain.model.sales;

import com.cybermanager.domain.model.shared.Money;

import java.util.Objects;

public record SaleLine(String label, int quantity, Money unitPrice, Money totalPrice) {
    public SaleLine {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Sale line label is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        Objects.requireNonNull(unitPrice, "unit price is required");
        Objects.requireNonNull(totalPrice, "total price is required");
    }
}

