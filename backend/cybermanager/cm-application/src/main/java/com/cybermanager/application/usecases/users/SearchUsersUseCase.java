package com.cybermanager.application.usecases.users;

import com.cybermanager.application.queries.users.SearchUsersQuery;
import com.cybermanager.application.views.users.UserSummaryView;

import java.util.List;

public interface SearchUsersUseCase {
    List<UserSummaryView> execute(SearchUsersQuery query);
}

