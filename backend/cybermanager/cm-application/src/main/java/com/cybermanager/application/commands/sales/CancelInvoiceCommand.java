package com.cybermanager.application.commands.sales;

import java.util.UUID;

public record CancelInvoiceCommand(UUID invoiceId) {
}
