package com.cybermanager.auth.api.dtos;

import java.util.Set;
import java.util.UUID;

public record CurrentUserResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        Set<String> roles
) {
}

