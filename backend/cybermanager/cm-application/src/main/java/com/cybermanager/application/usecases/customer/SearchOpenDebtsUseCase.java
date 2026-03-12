package com.cybermanager.application.usecases.customer;

import com.cybermanager.application.queries.customer.SearchOpenDebtsQuery;
import com.cybermanager.application.views.customer.CustomerDebtSummaryView;

import java.util.List;

public interface SearchOpenDebtsUseCase {
    List<CustomerDebtSummaryView> execute(SearchOpenDebtsQuery query);
}
