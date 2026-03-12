package com.cybermanager.domain.model.subscription;

import com.cybermanager.domain.model.shared.Money;

import java.util.Objects;

public record SubscriptionOffer(
        SubscriptionOfferId id,
        String name,
        Money price,
        int includedMinutes,
        SubscriptionOfferStatus status
) {
    public SubscriptionOffer {
        Objects.requireNonNull(id, "id is required");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Subscription offer name is required");
        }
        Objects.requireNonNull(price, "price is required");
        if (includedMinutes <= 0) {
            throw new IllegalArgumentException("Included duration must be positive");
        }
        Objects.requireNonNull(status, "status is required");
    }

    public static SubscriptionOffer create(String name, Money price, int includedMinutes) {
        return new SubscriptionOffer(SubscriptionOfferId.newId(), name.trim(), price, includedMinutes, SubscriptionOfferStatus.ACTIVE);
    }

    public SubscriptionOffer update(String name, Money price, int includedMinutes) {
        return new SubscriptionOffer(id, name, price, includedMinutes, status);
    }

    public SubscriptionOffer activate() {
        return new SubscriptionOffer(id, name, price, includedMinutes, SubscriptionOfferStatus.ACTIVE);
    }

    public SubscriptionOffer deactivate() {
        return new SubscriptionOffer(id, name, price, includedMinutes, SubscriptionOfferStatus.INACTIVE);
    }
}

