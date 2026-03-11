package com.cybermanager.application.usecases.customer;

import com.cybermanager.application.commands.customer.UpdateCustomerCommand;
import com.cybermanager.application.views.customer.CustomerView;

public interface UpdateCustomerUseCase {
    CustomerView execute(UpdateCustomerCommand command);
}
