package com.cybermanager.application.views.reporting;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CustomerDayHistoryView(
        UUID customerId,
        String name,
        List<SaleActivityView> sales,
        List<String> sessions,
        BigDecimal totalCollected,
        BigDecimal totalDebtCreated
) {
    public record SaleActivityView(String label, int quantity, BigDecimal totalPrice) {}
}
