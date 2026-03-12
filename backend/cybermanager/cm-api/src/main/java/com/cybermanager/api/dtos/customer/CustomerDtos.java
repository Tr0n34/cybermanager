package com.cybermanager.api.dtos.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class CustomerDtos {
    private CustomerDtos() {
    }

    public record CustomerRequest(String name, String type, UUID subscriptionOfferId) {}
    public record ConvertToSubscriberRequest(UUID subscriptionOfferId, boolean deductCurrentSession) {}
    public record CustomerResponse(UUID customerId, String name, String type, String status, int remainingMinutes, BigDecimal openDebtAmount) {}
    public record CustomerPurchaseResponse(UUID saleId, String type, String label, LocalDateTime soldAt, BigDecimal totalAmount, boolean openDebt) {}
    public record CustomerDebtResponse(UUID debtId, String label, BigDecimal amount, String status, LocalDateTime createdAt, LocalDateTime settledAt) {}
    public record CustomerDetailsResponse(
            UUID customerId,
            String name,
            String type,
            String status,
            int remainingMinutes,
            String currentSubscriptionLabel,
            List<CustomerPurchaseResponse> purchases,
            List<CustomerDebtResponse> debts
    ) {}
    public record DebtCustomerResponse(UUID customerId, String customerName, String customerType, BigDecimal totalOpenDebt, List<CustomerDebtResponse> debts) {}
    public record ConversionResponse(CustomerResponse customer, UUID saleId, int deductedMinutes) {}
}
