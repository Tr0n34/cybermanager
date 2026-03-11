package com.cybermanager.application.usecases.catalog;

import com.cybermanager.application.commands.catalog.UpdateProductCommand;
import com.cybermanager.application.views.catalog.ProductView;

public interface UpdateProductUseCase {
    ProductView execute(UpdateProductCommand command);
}
