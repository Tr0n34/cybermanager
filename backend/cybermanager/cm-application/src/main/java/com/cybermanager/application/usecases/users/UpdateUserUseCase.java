package com.cybermanager.application.usecases.users;

import com.cybermanager.application.commands.users.UpdateUserCommand;
import com.cybermanager.application.views.users.UserDetailsView;

public interface UpdateUserUseCase {
    UserDetailsView execute(UpdateUserCommand command);
}

