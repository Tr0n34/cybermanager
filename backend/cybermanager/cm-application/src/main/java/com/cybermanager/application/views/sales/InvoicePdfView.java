package com.cybermanager.application.views.sales;

import java.util.UUID;

public record InvoicePdfView(
        UUID invoiceId,
        String invoiceNumber,
        String fileName,
        String mediaType,
        byte[] content
) {
}
