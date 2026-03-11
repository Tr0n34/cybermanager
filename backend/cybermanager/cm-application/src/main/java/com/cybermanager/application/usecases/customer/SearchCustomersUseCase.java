package com.cybermanager.application.usecases.customer;

import com.cybermanager.application.queries.customer.SearchCustomersQuery;
import com.cybermanager.application.views.customer.CustomerView;

import java.util.List;

public interface SearchCustomersUseCase {
    List<CustomerView> execute(SearchCustomersQuery query);
}
