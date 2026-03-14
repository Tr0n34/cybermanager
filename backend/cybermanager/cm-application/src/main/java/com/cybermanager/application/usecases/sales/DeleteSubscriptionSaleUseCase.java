package com.cybermanager.application.usecases.sales;

import java.util.UUID;

public interface DeleteSubscriptionSaleUseCase {
    void execute(UUID saleId);
}
