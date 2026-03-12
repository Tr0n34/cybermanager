package com.cybermanager.application.commands.customer;

import java.util.Set;
import java.util.UUID;

public record CreateDebtFromSaleCommand(
        String actorEmail,
        Set<String> actorRoles,
        UUID saleId
) {
}
