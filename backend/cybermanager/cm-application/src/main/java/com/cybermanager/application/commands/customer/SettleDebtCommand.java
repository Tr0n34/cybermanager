package com.cybermanager.application.commands.customer;

import java.util.Set;
import java.util.UUID;

public record SettleDebtCommand(
        String actorEmail,
        Set<String> actorRoles,
        UUID debtId,
        String comment
) {
}
