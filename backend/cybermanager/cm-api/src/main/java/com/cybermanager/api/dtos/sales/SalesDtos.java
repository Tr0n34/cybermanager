package com.cybermanager.api.dtos.sales;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class SalesDtos {
    private SalesDtos() {
    }

    public record ProductSaleLineRequest(UUID productId, int quantity) {}
    public record ProductSaleRequest(UUID customerId, UUID sessionId, List<ProductSaleLineRequest> lines, boolean createDebt) {}
    public record SubscriptionSaleRequest(UUID customerId, UUID sessionId, UUID subscriptionOfferId, Integer quantity, boolean createDebt) {}
    public record ConnectionTimeSaleRequest(UUID customerId, UUID sessionId, int minutes, boolean createDebt) {}
    public record PricingTierRequest(int hours, int minutes, BigDecimal price) {}
    public record PricingRequest(List<PricingTierRequest> tiers) {}
    public record SaleLineResponse(String label, int quantity, BigDecimal unitPrice, BigDecimal totalPrice) {}
    public record SaleResponse(UUID saleId, UUID customerId, String type, LocalDateTime soldAt, List<SaleLineResponse> lines, BigDecimal totalAmount) {}
    public record PricingTierResponse(Long id, int hours, int minutes, int durationMinutes, BigDecimal price, LocalDateTime createdAt, LocalDateTime updatedAt) {}
    public record PricingResponse(List<PricingTierResponse> tiers) {}
}

