package com.cybermanager.application.views.customer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CustomerDebtSummaryView(
        UUID customerId,
        String customerName,
        String customerType,
        BigDecimal totalOpenDebt,
        List<CustomerDebtView> debts
) {
}
