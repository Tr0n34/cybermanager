package com.cybermanager.application.usecases.session;

import com.cybermanager.application.commands.session.PaySessionCommand;
import com.cybermanager.application.views.session.SessionView;

public interface PaySessionUseCase {
    SessionView execute(PaySessionCommand command);
}
