package com.cybermanager.api.dtos.monitoring;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class MonitoringDtos {
    private MonitoringDtos() {
    }

    public record DayCustomerResponse(
            UUID customerId,
            String name,
            String type,
            int remainingMinutes,
            int consumedMinutes,
            BigDecimal purchasesTotal,
            BigDecimal debtTotal,
            BigDecimal collectedTotal,
            String state
    ) {}
    public record SaleActivityResponse(String label, int quantity, BigDecimal totalPrice) {}
    public record DayCustomerActivityResponse(UUID customerId, String name, List<SaleActivityResponse> sales, List<String> sessions, BigDecimal totalCollected, BigDecimal totalDebtCreated) {}
}

