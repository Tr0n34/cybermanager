package com.cybermanager.application.queries.users;

import java.util.Set;
import java.util.UUID;

public record GetUserDetailsQuery(
        String actorEmail,
        Set<String> actorRoles,
        UUID userId
) {
}

