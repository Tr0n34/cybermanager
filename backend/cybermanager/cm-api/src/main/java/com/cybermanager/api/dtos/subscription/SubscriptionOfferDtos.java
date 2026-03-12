package com.cybermanager.api.dtos.subscription;

import java.math.BigDecimal;
import java.util.UUID;

public final class SubscriptionOfferDtos {
    private SubscriptionOfferDtos() {
    }

    public record SubscriptionOfferRequest(String name, BigDecimal price, int includedMinutes) {}
    public record SubscriptionOfferResponse(UUID offerId, String name, BigDecimal price, int includedMinutes, String status) {}
}

