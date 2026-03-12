package com.cybermanager.application.usecases.users;

import com.cybermanager.application.commands.users.ChangeUserStatusCommand;
import com.cybermanager.application.views.users.UserDetailsView;

public interface EnableUserUseCase {
    UserDetailsView execute(ChangeUserStatusCommand command);
}

