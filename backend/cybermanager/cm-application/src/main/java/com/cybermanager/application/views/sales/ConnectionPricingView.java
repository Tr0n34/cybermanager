package com.cybermanager.application.views.sales;

import java.math.BigDecimal;
import java.util.List;

public record ConnectionPricingView(List<PricingTierView> tiers) {
    public record PricingTierView(int hours, int minutes, int durationMinutes, BigDecimal price) {
    }
}
