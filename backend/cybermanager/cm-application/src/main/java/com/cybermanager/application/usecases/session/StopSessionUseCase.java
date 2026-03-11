package com.cybermanager.application.usecases.session;

import com.cybermanager.application.commands.session.StopSessionCommand;
import com.cybermanager.application.views.session.SessionView;

public interface StopSessionUseCase {
    SessionView execute(StopSessionCommand command);
}
