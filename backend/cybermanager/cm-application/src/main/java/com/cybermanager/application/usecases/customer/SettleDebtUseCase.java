package com.cybermanager.application.usecases.customer;

import com.cybermanager.application.commands.customer.SettleDebtCommand;

public interface SettleDebtUseCase {
    void execute(SettleDebtCommand command);
}
