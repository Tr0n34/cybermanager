package com.cybermanager.application.usecases.catalog;

import com.cybermanager.application.commands.catalog.DeactivateProductCommand;
import com.cybermanager.application.views.catalog.ProductView;

public interface DeactivateProductUseCase {
    ProductView execute(DeactivateProductCommand command);
}
