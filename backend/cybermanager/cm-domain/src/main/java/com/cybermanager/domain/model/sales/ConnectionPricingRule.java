package com.cybermanager.domain.model.sales;

import com.cybermanager.domain.model.shared.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record ConnectionPricingRule(List<ConnectionPricingTier> tiers) {
    public ConnectionPricingRule {
        Objects.requireNonNull(tiers, "tiers are required");
        if (tiers.isEmpty()) {
            throw new IllegalArgumentException("At least one pricing tier is required");
        }
        var sortedTiers = tiers.stream()
                .sorted(Comparator.comparingInt(ConnectionPricingTier::durationMinutes))
                .toList();
        for (int index = 1; index < sortedTiers.size(); index++) {
            if (sortedTiers.get(index).durationMinutes() == sortedTiers.get(index - 1).durationMinutes()) {
                throw new IllegalArgumentException("Pricing tier durations must be unique");
            }
        }
        tiers = List.copyOf(sortedTiers);
    }

    public Money priceForMinutes(int minutes) {
        if (minutes <= 0) {
            throw new IllegalArgumentException("Minutes must be positive");
        }

        int maxDuration = tiers.get(tiers.size() - 1).durationMinutes();
        int limit = minutes + maxDuration;
        List<BigDecimal> prices = new ArrayList<>(limit + 1);
        for (int current = 0; current <= limit; current++) {
            prices.add(null);
        }
        prices.set(0, BigDecimal.ZERO);

        for (int coveredMinutes = 1; coveredMinutes <= limit; coveredMinutes++) {
            BigDecimal bestPrice = null;
            for (ConnectionPricingTier tier : tiers) {
                int previousMinutes = Math.max(0, coveredMinutes - tier.durationMinutes());
                BigDecimal previousPrice = prices.get(previousMinutes);
                if (previousPrice == null) {
                    continue;
                }
                BigDecimal candidate = previousPrice.add(tier.price().amount());
                if (bestPrice == null || candidate.compareTo(bestPrice) < 0) {
                    bestPrice = candidate;
                }
            }
            prices.set(coveredMinutes, bestPrice);
        }

        BigDecimal bestPrice = null;
        for (int coveredMinutes = minutes; coveredMinutes <= limit; coveredMinutes++) {
            BigDecimal candidate = prices.get(coveredMinutes);
            if (candidate != null && (bestPrice == null || candidate.compareTo(bestPrice) < 0)) {
                bestPrice = candidate;
            }
        }
        if (bestPrice == null) {
            throw new IllegalStateException("Unable to calculate pricing for duration");
        }
        return new Money(bestPrice);
    }
}

