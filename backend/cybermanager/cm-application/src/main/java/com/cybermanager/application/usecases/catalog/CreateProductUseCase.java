package com.cybermanager.application.usecases.catalog;

import com.cybermanager.application.commands.catalog.CreateProductCommand;
import com.cybermanager.application.views.catalog.ProductView;

public interface CreateProductUseCase {
    ProductView execute(CreateProductCommand command);
}
