package com.cybermanager.application.views.sales;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SaleView(UUID saleId, UUID customerId, String type, LocalDateTime soldAt, List<SaleLineView> lines, BigDecimal totalAmount) {
}
