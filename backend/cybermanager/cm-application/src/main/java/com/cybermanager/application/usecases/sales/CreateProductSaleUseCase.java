package com.cybermanager.application.usecases.sales;

import com.cybermanager.application.commands.sales.CreateProductSaleCommand;
import com.cybermanager.application.views.sales.SaleView;

public interface CreateProductSaleUseCase {
    SaleView execute(CreateProductSaleCommand command);
}
