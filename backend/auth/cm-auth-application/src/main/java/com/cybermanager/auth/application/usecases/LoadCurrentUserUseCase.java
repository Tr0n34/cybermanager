package com.cybermanager.auth.application.usecases;

import com.cybermanager.auth.application.queries.LoadCurrentUserQuery;
import com.cybermanager.auth.application.views.CurrentUserView;

public interface LoadCurrentUserUseCase {
    CurrentUserView execute(LoadCurrentUserQuery query);
}

