package com.cybermanager.application.views.subscription;

import java.math.BigDecimal;
import java.util.UUID;

public record SubscriptionOfferView(UUID offerId, String name, BigDecimal price, int includedMinutes, String status) {
}
