package com.cybermanager.application.usecases.users;

import com.cybermanager.application.commands.users.DeleteUserCommand;

public interface DeleteUserUseCase {
    void execute(DeleteUserCommand command);
}
