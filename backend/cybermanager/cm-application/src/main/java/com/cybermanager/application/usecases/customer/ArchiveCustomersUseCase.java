package com.cybermanager.application.usecases.customer;

import com.cybermanager.application.commands.customer.ArchiveCustomersCommand;
import com.cybermanager.application.views.customer.CustomerArchiveResultView;

public interface ArchiveCustomersUseCase {
    CustomerArchiveResultView execute(ArchiveCustomersCommand command);
}
