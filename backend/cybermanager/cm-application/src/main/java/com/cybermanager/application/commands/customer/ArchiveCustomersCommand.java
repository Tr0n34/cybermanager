package com.cybermanager.application.commands.customer;

import java.time.LocalDate;
import java.util.Set;

public record ArchiveCustomersCommand(
        String actorEmail,
        Set<String> actorRoles,
        LocalDate startDate,
        LocalDate endDate,
        String type,
        String format
) {
}
