package com.cybermanager.application.usecases.sales;

import com.cybermanager.application.queries.sales.GetSaleDetailsQuery;
import com.cybermanager.application.views.sales.SaleView;

public interface GetSaleDetailsUseCase {
    SaleView execute(GetSaleDetailsQuery query);
}
