package com.cybermanager.application.commands.customer;

import java.util.Set;
import java.util.UUID;

public record ConvertCustomerToSubscriberCommand(
        String actorEmail,
        Set<String> actorRoles,
        UUID customerId,
        UUID subscriptionOfferId,
        boolean deductCurrentSession
) {
}
