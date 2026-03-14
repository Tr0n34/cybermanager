package com.cybermanager.application.usecases.session;

import com.cybermanager.application.commands.session.PauseSessionCommand;
import com.cybermanager.application.views.session.SessionView;

public interface PauseSessionUseCase {
    SessionView execute(PauseSessionCommand command);
}
