package com.cybermanager.application.usecases.users;

import com.cybermanager.application.queries.users.GetUserDetailsQuery;
import com.cybermanager.application.views.users.UserDetailsView;

public interface GetUserDetailsUseCase {
    UserDetailsView execute(GetUserDetailsQuery query);
}

