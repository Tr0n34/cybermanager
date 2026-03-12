package com.cybermanager.auth.application.views;

import java.util.Set;
import java.util.UUID;

public record CurrentUserView(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        Set<String> roles
) {
}

