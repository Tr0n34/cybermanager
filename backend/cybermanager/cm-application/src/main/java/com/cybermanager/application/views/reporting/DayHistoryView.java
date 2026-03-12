package com.cybermanager.application.views.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DayHistoryView(LocalDate startDate, LocalDate endDate, List<DayCustomerHistoryView> customers, BigDecimal totalCollected, BigDecimal totalDebtCreated) {
}
