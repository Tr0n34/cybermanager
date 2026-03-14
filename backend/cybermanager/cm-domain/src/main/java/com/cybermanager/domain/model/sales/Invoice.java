package com.cybermanager.domain.model.sales;

import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.shared.Money;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record Invoice(
        UUID id,
        UUID saleId,
        String invoiceNumber,
        CustomerId customerId,
        String customerName,
        LocalDateTime issuedAt,
        InvoiceStatus status,
        Money totalAmount,
        List<InvoiceLine> lines
) {
}
