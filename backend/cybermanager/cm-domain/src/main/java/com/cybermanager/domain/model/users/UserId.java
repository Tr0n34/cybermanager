package com.cybermanager.domain.model.users;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {
    public UserId {
        Objects.requireNonNull(value, "value is required");
    }

    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }
}

