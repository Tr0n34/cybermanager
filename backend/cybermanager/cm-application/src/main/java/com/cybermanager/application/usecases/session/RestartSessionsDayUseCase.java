package com.cybermanager.application.usecases.session;

import com.cybermanager.application.commands.session.RestartSessionsDayCommand;

public interface RestartSessionsDayUseCase {
    int execute(RestartSessionsDayCommand command);
}
