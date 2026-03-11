package com.cybermanager.application.views.session;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SessionView(
        UUID sessionId,
        UUID customerId,
        String customerName,
        String customerType,
        int remainingMinutes,
        String workstation,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        int consumedMinutes,
        BigDecimal calculatedPrice
) {
}
