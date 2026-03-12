package com.cybermanager.application.commands.users;

import java.util.Set;
import java.util.UUID;

public record DeleteUserCommand(
        String actorEmail,
        Set<String> actorRoles,
        UUID userId
) {
}
