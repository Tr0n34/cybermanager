package com.cybermanager.application.commands.sales;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CreateProductSaleCommand(String actorEmail, Set<String> actorRoles, UUID customerId, List<ProductSaleLineCommand> lines) {
    public record ProductSaleLineCommand(UUID productId, int quantity) {
    }
}
