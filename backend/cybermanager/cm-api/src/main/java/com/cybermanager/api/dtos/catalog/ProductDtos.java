package com.cybermanager.api.dtos.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public final class ProductDtos {
    private ProductDtos() {
    }

    public record ProductRequest(String name, BigDecimal price, String category) {}
    public record ProductResponse(UUID productId, String name, BigDecimal price, String category, String status) {}
}

