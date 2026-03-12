package com.cybermanager.application.usecases.catalog;

import com.cybermanager.application.commands.catalog.DeleteProductCommand;

public interface DeleteProductUseCase {
    void execute(DeleteProductCommand command);
}
