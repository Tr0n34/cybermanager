package com.cybermanager.application.usecases.catalog;

import com.cybermanager.application.commands.catalog.ActivateProductCommand;
import com.cybermanager.application.views.catalog.ProductView;

public interface ActivateProductUseCase {
    ProductView execute(ActivateProductCommand command);
}
