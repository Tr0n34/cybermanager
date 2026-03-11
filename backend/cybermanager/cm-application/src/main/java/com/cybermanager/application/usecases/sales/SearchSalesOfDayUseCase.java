package com.cybermanager.application.usecases.sales;

import com.cybermanager.application.queries.sales.SearchSalesOfDayQuery;
import com.cybermanager.application.views.sales.SaleView;

import java.util.List;

public interface SearchSalesOfDayUseCase {
    List<SaleView> execute(SearchSalesOfDayQuery query);
}
