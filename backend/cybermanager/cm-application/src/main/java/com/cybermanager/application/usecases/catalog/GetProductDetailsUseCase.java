package com.cybermanager.application.usecases.catalog;

import com.cybermanager.application.queries.catalog.GetProductDetailsQuery;
import com.cybermanager.application.views.catalog.ProductView;

public interface GetProductDetailsUseCase {
    ProductView execute(GetProductDetailsQuery query);
}
