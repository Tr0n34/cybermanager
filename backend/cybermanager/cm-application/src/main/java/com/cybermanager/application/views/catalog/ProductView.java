package com.cybermanager.application.views.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductView(UUID productId, String name, BigDecimal price, String category, String status) {
}
