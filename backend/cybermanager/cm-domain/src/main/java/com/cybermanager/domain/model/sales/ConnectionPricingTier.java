package com.cybermanager.domain.model.sales;

import com.cybermanager.domain.model.shared.Money;

import java.util.Objects;

public record ConnectionPricingTier(int durationMinutes, Money price) {
    public ConnectionPricingTier {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration minutes must be positive");
        }
        Objects.requireNonNull(price, "price is required");
    }
}
