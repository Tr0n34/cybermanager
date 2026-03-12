package com.cybermanager.application.services.users;

import com.cybermanager.application.queries.users.SearchUsersQuery;
import com.cybermanager.application.usecases.users.SearchUsersUseCase;
import com.cybermanager.application.views.users.UserSummaryView;
import com.cybermanager.domain.port.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SearchUsersService implements SearchUsersUseCase {
    private final UserRepository userRepository;

    public SearchUsersService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<UserSummaryView> execute(SearchUsersQuery query) {
        UsersApplicationSupport.requireAdmin(query.actorRoles());
        return userRepository.search(query.term(), UsersApplicationSupport.status(query.status()))
                .stream()
                .map(UsersApplicationSupport::toSummary)
                .toList();
    }
}

