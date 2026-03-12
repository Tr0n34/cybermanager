package com.cybermanager.application.views.customer;

import java.util.List;
import java.util.UUID;

public record CustomerDetailsView(
        UUID customerId,
        String name,
        String type,
        String status,
        int remainingMinutes,
        String currentSubscriptionLabel,
        List<CustomerSaleView> purchases,
        List<CustomerDebtView> debts
) {
}
