package com.cybermanager.application.usecases.sales;

import com.cybermanager.application.commands.sales.CreateConnectionTimeSaleCommand;
import com.cybermanager.application.views.sales.SaleView;

public interface CreateConnectionTimeSaleUseCase {
    SaleView execute(CreateConnectionTimeSaleCommand command);
}
