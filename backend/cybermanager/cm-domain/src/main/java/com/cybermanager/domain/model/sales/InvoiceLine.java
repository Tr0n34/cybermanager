package com.cybermanager.domain.model.sales;

import com.cybermanager.domain.model.shared.Money;

public record InvoiceLine(
        String label,
        int quantity,
        Money unitPrice,
        Money totalPrice
) {
}
