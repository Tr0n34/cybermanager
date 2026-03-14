package com.cybermanager.application.views.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerSaleView(UUID saleId, UUID sessionId, String type, String label, String debtLabel, LocalDateTime soldAt, BigDecimal totalAmount, boolean openDebt) {
}
