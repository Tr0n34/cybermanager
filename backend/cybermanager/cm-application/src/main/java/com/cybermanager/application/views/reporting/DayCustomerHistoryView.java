package com.cybermanager.application.views.reporting;

import java.math.BigDecimal;
import java.util.UUID;

public record DayCustomerHistoryView(UUID customerId, String name, String type, int totalMinutes, BigDecimal salesTotal) {
}
