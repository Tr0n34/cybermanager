package com.cybermanager.application.usecases.users;

import com.cybermanager.application.commands.users.ChangeUserStatusCommand;
import com.cybermanager.application.views.users.UserDetailsView;

public interface DisableUserUseCase {
    UserDetailsView execute(ChangeUserStatusCommand command);
}

