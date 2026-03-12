package com.cybermanager.api.dtos.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ReportingDtos {
    private ReportingDtos() {
    }

    public record DayCustomerHistoryResponse(
            UUID customerId,
            String name,
            String type,
            int totalMinutes,
            BigDecimal salesTotal,
            BigDecimal debtTotal,
            BigDecimal collectedTotal,
            String state
    ) {}
    public record DayHistoryResponse(LocalDate startDate, LocalDate endDate, List<DayCustomerHistoryResponse> customers, BigDecimal totalCollected, BigDecimal totalDebtCreated) {}
    public record SaleActivityResponse(String label, int quantity, BigDecimal totalPrice) {}
    public record CustomerDayHistoryResponse(UUID customerId, String name, List<SaleActivityResponse> sales, List<String> sessions, BigDecimal totalCollected, BigDecimal totalDebtCreated) {}
}

