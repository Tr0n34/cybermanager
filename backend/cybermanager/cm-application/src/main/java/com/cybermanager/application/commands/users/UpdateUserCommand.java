package com.cybermanager.application.commands.users;

import java.util.Set;
import java.util.UUID;

public record UpdateUserCommand(
        String actorEmail,
        Set<String> actorRoles,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String password,
        Set<String> roles
) {
}

