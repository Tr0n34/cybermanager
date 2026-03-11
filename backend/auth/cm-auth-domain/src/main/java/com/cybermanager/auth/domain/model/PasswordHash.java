package com.cybermanager.auth.domain.model;

import java.util.Objects;

public record PasswordHash(String value) {
    public PasswordHash {
        Objects.requireNonNull(value, "value is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Password hash is required");
        }
    }
}

