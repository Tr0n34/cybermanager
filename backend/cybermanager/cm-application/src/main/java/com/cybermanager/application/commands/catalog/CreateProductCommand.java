package com.cybermanager.application.commands.catalog;

import java.math.BigDecimal;
import java.util.Set;

public record CreateProductCommand(String actorEmail, Set<String> actorRoles, String name, BigDecimal price, String category) {
}
