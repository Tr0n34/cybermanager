package com.cybermanager.application.views.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerDebtView(
        UUID debtId,
        String label,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime settledAt
) {
}
