package com.cybermanager.application.views.sales;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record InvoiceDetailView(
        UUID invoiceId,
        UUID saleId,
        String invoiceNumber,
        UUID customerId,
        String customerName,
        LocalDateTime issuedAt,
        String status,
        BigDecimal totalAmount,
        List<InvoiceLineView> lines
) {
}
