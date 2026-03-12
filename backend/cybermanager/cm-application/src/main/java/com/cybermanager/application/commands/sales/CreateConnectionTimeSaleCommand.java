package com.cybermanager.application.commands.sales;

import java.util.Set;
import java.util.UUID;

public record CreateConnectionTimeSaleCommand(String actorEmail, Set<String> actorRoles, UUID customerId, int minutes, boolean createDebt) {
}
