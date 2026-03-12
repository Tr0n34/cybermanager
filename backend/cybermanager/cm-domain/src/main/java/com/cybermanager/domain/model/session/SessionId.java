package com.cybermanager.domain.model.session;

import java.util.UUID;

public record SessionId(UUID value) {
    public static SessionId newId() {
        return new SessionId(UUID.randomUUID());
    }
}

