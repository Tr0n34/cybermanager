package com.cybermanager.application.usecases.users;

import com.cybermanager.application.commands.users.CreateUserCommand;
import com.cybermanager.application.views.users.UserDetailsView;

public interface CreateUserUseCase {
    UserDetailsView execute(CreateUserCommand command);
}

