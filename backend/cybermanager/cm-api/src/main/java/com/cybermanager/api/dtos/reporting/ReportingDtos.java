package com.cybermanager.api.dtos.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ReportingDtos {
    private ReportingDtos() {
    }

    public record DayCustomerHistoryResponse(UUID customerId, String name, String type, int totalMinutes, BigDecimal salesTotal) {}
    public record DayHistoryResponse(LocalDate date, List<DayCustomerHistoryResponse> customers) {}
    public record CustomerDayHistoryResponse(UUID customerId, String name, List<String> sales, List<String> sessions) {}
}

