package com.cybermanager.api.shared;

import com.cybermanager.infrastructure.security.users.AuthenticatedActor;
import com.cybermanager.infrastructure.security.users.JwtAccessTokenReader;

public final class ApiSupport {
    private ApiSupport() {
    }

    public static String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing bearer token");
        }
        return authorization.substring(7);
    }

    public static AuthenticatedActor actor(String authorization, JwtAccessTokenReader tokenReader) {
        return tokenReader.read(extractToken(authorization));
    }
}

