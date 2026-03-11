package com.cybermanager.application.usecases.customer;

import com.cybermanager.application.commands.customer.CreateCustomerCommand;
import com.cybermanager.application.views.customer.CustomerView;

public interface CreateCustomerUseCase {
    CustomerView execute(CreateCustomerCommand command);
}
