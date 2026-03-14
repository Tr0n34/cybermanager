package com.cybermanager.application.usecases.customer;

import com.cybermanager.application.commands.customer.ReattachDebtToSessionCommand;

public interface ReattachDebtToSessionUseCase {
    void execute(ReattachDebtToSessionCommand command);
}
