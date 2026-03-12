package com.cybermanager.application.views.monitoring;

import java.math.BigDecimal;
import java.util.UUID;

public record DailyCustomerView(
        UUID customerId,
        String name,
        String type,
        int remainingMinutes,
        int consumedMinutes,
        BigDecimal purchasesTotal,
        BigDecimal debtTotal,
        BigDecimal collectedTotal,
        String sessionState
) {
}
