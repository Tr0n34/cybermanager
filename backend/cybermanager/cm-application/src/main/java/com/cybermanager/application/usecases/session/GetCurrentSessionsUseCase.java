package com.cybermanager.application.usecases.session;

import com.cybermanager.application.views.session.CurrentSessionsView;

public interface GetCurrentSessionsUseCase {
    CurrentSessionsView execute();
}
