package com.cybermanager.auth.application.usecases;

import com.cybermanager.auth.application.commands.AuthenticateUserCommand;
import com.cybermanager.auth.application.views.AuthenticationResultView;

public interface AuthenticateUserUseCase {
    AuthenticationResultView execute(AuthenticateUserCommand command);
}

