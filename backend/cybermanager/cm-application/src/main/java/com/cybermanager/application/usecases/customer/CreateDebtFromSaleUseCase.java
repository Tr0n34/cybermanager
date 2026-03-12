package com.cybermanager.application.usecases.customer;

import com.cybermanager.application.commands.customer.CreateDebtFromSaleCommand;

public interface CreateDebtFromSaleUseCase {
    void execute(CreateDebtFromSaleCommand command);
}
