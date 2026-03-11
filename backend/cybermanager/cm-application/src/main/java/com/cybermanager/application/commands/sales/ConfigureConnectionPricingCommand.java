package com.cybermanager.application.commands.sales;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record ConfigureConnectionPricingCommand(
        String actorEmail,
        Set<String> actorRoles,
        List<PricingTierCommand> tiers
) {
    public record PricingTierCommand(int hours, int minutes, BigDecimal price) {
    }
}
