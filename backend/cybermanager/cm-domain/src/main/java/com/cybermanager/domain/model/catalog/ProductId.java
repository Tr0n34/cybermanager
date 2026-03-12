package com.cybermanager.domain.model.catalog;

import java.util.UUID;

public record ProductId(UUID value) {
    public static ProductId newId() {
        return new ProductId(UUID.randomUUID());
    }
}

