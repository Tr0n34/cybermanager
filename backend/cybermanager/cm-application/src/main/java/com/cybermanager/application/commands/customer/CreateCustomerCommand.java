package com.cybermanager.application.commands.customer;

import java.util.Set;
import java.util.UUID;

public record CreateCustomerCommand(
        String actorEmail,
        Set<String> actorRoles,
        String name,
        String type,
        UUID subscriptionOfferId
) {
}
