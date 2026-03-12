package com.cybermanager.domain.model.users;

import java.util.Locale;
import java.util.Objects;

public record EmailAddress(String value) {
    public EmailAddress {
        Objects.requireNonNull(value, "value is required");
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Email format is invalid");
        }
        value = normalized;
    }
}

