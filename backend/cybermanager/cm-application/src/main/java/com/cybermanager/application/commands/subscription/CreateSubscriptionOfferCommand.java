package com.cybermanager.application.commands.subscription;

import java.math.BigDecimal;
import java.util.Set;

public record CreateSubscriptionOfferCommand(String actorEmail, Set<String> actorRoles, String name, BigDecimal price, int includedMinutes) {
}
