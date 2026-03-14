package com.cybermanager.application.views.sales;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

public record ConnectionPricingView(List<PricingTierView> tiers) {
    public record PricingTierView(Long id, int hours, int minutes, int durationMinutes, BigDecimal price, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
