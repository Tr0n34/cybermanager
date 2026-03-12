package com.cybermanager.application.views.customer;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomerView(UUID customerId, String name, String type, String status, int remainingMinutes, BigDecimal openDebtAmount) {
}
