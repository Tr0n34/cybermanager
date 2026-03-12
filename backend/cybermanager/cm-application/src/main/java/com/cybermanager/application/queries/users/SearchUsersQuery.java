package com.cybermanager.application.queries.users;

import java.util.Set;

public record SearchUsersQuery(
        String actorEmail,
        Set<String> actorRoles,
        String term,
        String status
) {
}

