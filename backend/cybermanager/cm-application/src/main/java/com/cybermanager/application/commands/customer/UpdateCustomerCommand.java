package com.cybermanager.application.commands.customer;

import java.util.Set;
import java.util.UUID;

public record UpdateCustomerCommand(String actorEmail, Set<String> actorRoles, UUID customerId, String name) {
}
