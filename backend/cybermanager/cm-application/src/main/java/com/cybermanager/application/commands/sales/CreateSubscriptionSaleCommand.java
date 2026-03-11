package com.cybermanager.application.commands.sales;

import java.util.Set;
import java.util.UUID;

public record CreateSubscriptionSaleCommand(String actorEmail, Set<String> actorRoles, UUID customerId, UUID subscriptionOfferId) {
}
