package com.cybermanager.application.usecases.session;

import com.cybermanager.application.commands.session.StartSessionCommand;
import com.cybermanager.application.views.session.SessionView;

public interface StartSessionUseCase {
    SessionView execute(StartSessionCommand command);
}
