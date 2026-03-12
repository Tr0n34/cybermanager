package com.cybermanager.application.commands.users;

import java.util.Set;

public record CreateUserCommand(
        String actorEmail,
        Set<String> actorRoles,
        String email,
        String firstName,
        String lastName,
        String password,
        Set<String> roles
) {
}

