package com.cybermanager.application.commands.subscription;

import java.util.Set;
import java.util.UUID;

public record ActivateSubscriptionOfferCommand(String actorEmail, Set<String> actorRoles, UUID offerId) {
}
