package com.cybermanager.application.views.users;

import java.util.Set;
import java.util.UUID;

public record UserDetailsView(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        Set<String> roles,
        String status
) {
}

