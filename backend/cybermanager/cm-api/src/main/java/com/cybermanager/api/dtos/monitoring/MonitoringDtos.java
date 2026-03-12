package com.cybermanager.api.dtos.monitoring;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class MonitoringDtos {
    private MonitoringDtos() {
    }

    public record DayCustomerResponse(UUID customerId, String name, String type, int remainingMinutes, int consumedMinutes, BigDecimal purchasesTotal, boolean activeSession) {}
    public record DayCustomerActivityResponse(UUID customerId, String name, List<String> sales, List<String> sessions) {}
}

