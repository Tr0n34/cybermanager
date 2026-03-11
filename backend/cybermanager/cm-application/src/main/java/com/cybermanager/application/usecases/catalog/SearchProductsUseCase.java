package com.cybermanager.application.usecases.catalog;

import com.cybermanager.application.queries.catalog.SearchProductsQuery;
import com.cybermanager.application.views.catalog.ProductView;

import java.util.List;

public interface SearchProductsUseCase {
    List<ProductView> execute(SearchProductsQuery query);
}
