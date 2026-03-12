package com.cybermanager.domain.model.subscription;

import java.util.UUID;

public record SubscriptionOfferId(UUID value) {
    public static SubscriptionOfferId newId() {
        return new SubscriptionOfferId(UUID.randomUUID());
    }
}

