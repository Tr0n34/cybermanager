package com.cybermanager.auth.api.dtos;

import java.util.Set;
import java.util.UUID;

public record AuthenticationResponse(
        String token,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        Set<String> roles
) {
}

