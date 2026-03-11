package com.cybermanager.application.commands.catalog;

import java.util.Set;
import java.util.UUID;

public record ActivateProductCommand(String actorEmail, Set<String> actorRoles, UUID productId) {
}
