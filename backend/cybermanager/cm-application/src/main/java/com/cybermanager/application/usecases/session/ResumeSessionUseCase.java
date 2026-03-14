package com.cybermanager.application.usecases.session;

import com.cybermanager.application.commands.session.ResumeSessionCommand;
import com.cybermanager.application.views.session.SessionView;

public interface ResumeSessionUseCase {
    SessionView execute(ResumeSessionCommand command);
}
