package com.cybermanager.application.commands.sales;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateManualInvoiceCommand(
        String actorEmail,
        UUID customerId,
        String customerName,
        List<ManualInvoiceLineCommand> lines
) {
    public record ManualInvoiceLineCommand(String label, int quantity, BigDecimal unitPrice) {
    }
}
