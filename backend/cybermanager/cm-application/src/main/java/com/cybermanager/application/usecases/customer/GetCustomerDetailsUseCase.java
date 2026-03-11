package com.cybermanager.application.usecases.customer;

import com.cybermanager.application.queries.customer.GetCustomerDetailsQuery;
import com.cybermanager.application.views.customer.CustomerDetailsView;

public interface GetCustomerDetailsUseCase {
    CustomerDetailsView execute(GetCustomerDetailsQuery query);
}
