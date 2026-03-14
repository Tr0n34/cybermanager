package com.cybermanager.application.commands.sales;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CreateProductSaleCommand(String actorEmail, Set<String> actorRoles, UUID customerId, UUID sessionId, List<ProductSaleLineCommand> lines, boolean createDebt) {
    public record ProductSaleLineCommand(UUID productId, int quantity) {
    }
}
