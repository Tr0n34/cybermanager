package com.cybermanager.api.dtos.sales;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class InvoiceDtos {
    private InvoiceDtos() {
    }

    public record InvoiceLineResponse(String label, int quantity, BigDecimal unitPrice, BigDecimal totalPrice) {}

    public record ManualInvoiceLineRequest(String label, int quantity, BigDecimal unitPrice) {}

    public record CreateInvoiceRequest(UUID customerId, String customerName, List<ManualInvoiceLineRequest> lines) {}

    public record InvoiceSummaryResponse(
            UUID invoiceId,
            UUID saleId,
            String invoiceNumber,
            String customerName,
            LocalDateTime issuedAt,
            String status,
            BigDecimal totalAmount
    ) {}

    public record InvoiceDetailResponse(
            UUID invoiceId,
            UUID saleId,
            UUID customerId,
            String invoiceNumber,
            String customerName,
            LocalDateTime issuedAt,
            String status,
            BigDecimal totalAmount,
            List<InvoiceLineResponse> lines
    ) {}
}
