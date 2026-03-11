package com.cybermanager.application.views.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerSaleView(UUID saleId, String type, String label, LocalDateTime soldAt, BigDecimal totalAmount) {
}
