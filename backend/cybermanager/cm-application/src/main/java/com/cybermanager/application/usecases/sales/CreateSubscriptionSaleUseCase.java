package com.cybermanager.application.usecases.sales;

import com.cybermanager.application.commands.sales.CreateSubscriptionSaleCommand;
import com.cybermanager.application.views.sales.SaleView;

public interface CreateSubscriptionSaleUseCase {
    SaleView execute(CreateSubscriptionSaleCommand command);
}
