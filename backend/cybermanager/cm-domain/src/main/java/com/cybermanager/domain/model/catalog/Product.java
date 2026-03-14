package com.cybermanager.domain.model.catalog;

import com.cybermanager.domain.model.shared.Money;

import java.util.Objects;

public record Product(
        ProductId id,
        String name,
        Money price,
        String category,
        String description,
        ProductStatus status
) {
    public Product {
        Objects.requireNonNull(id, "id is required");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        Objects.requireNonNull(price, "price is required");
        category = category == null ? "GENERAL" : category.trim();
        description = description == null ? "" : description.trim();
        Objects.requireNonNull(status, "status is required");
    }

    public static Product create(String name, Money price, String category, String description) {
        return new Product(ProductId.newId(), name.trim(), price, category, description, ProductStatus.ACTIVE);
    }

    public Product update(String name, Money price, String category, String description) {
        return new Product(id, name, price, category, description, status);
    }

    public Product activate() {
        return new Product(id, name, price, category, description, ProductStatus.ACTIVE);
    }

    public Product deactivate() {
        return new Product(id, name, price, category, description, ProductStatus.INACTIVE);
    }
}

