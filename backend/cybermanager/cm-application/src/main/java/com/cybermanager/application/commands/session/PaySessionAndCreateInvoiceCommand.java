package com.cybermanager.application.commands.session;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PaySessionAndCreateInvoiceCommand(
        UUID sessionId,
        BigDecimal amountPaid,
        List<UUID> subscriptionOfferIds,
        boolean createSubscriptionDebt
) {
}
