package com.cybermanager.application.usecases.session;

import com.cybermanager.application.queries.session.SearchSessionsOfDayQuery;
import com.cybermanager.application.views.session.SessionView;

import java.util.List;

public interface SearchSessionsOfDayUseCase {
    List<SessionView> execute(SearchSessionsOfDayQuery query);
}
