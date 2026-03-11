package com.cybermanager.application.views.sales;

import java.math.BigDecimal;

public record SaleLineView(String label, int quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
}
