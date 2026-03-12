package com.cybermanager.api.dtos.users;

import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        Set<String> roles,
        String status
) {
}

