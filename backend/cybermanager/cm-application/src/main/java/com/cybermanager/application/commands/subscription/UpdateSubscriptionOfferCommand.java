package com.cybermanager.application.commands.subscription;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record UpdateSubscriptionOfferCommand(String actorEmail, Set<String> actorRoles, UUID offerId, String name, BigDecimal price, int includedMinutes) {
}
