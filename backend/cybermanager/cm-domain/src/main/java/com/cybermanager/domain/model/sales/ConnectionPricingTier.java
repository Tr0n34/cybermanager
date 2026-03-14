package com.cybermanager.domain.model.sales;

import com.cybermanager.domain.model.shared.Money;

import java.time.LocalDateTime;
import java.util.Objects;

public record ConnectionPricingTier(Long id, int durationMinutes, Money price, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public ConnectionPricingTier(int durationMinutes, Money price) {
        this(null, durationMinutes, price, null, null);
    }

    public ConnectionPricingTier {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration minutes must be positive");
        }
        Objects.requireNonNull(price, "price is required");
    }
}
