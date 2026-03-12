package com.cybermanager.application.services.users;

import com.cybermanager.application.queries.users.GetUserDetailsQuery;
import com.cybermanager.application.services.shared.BusinessErrorType;
import com.cybermanager.application.usecases.users.GetUserDetailsUseCase;
import com.cybermanager.application.views.users.UserDetailsView;
import com.cybermanager.domain.port.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetUserDetailsService implements GetUserDetailsUseCase {
    private final UserRepository userRepository;

    public GetUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetailsView execute(GetUserDetailsQuery query) {
        UsersApplicationSupport.requireAdmin(query.actorRoles());
        var user = userRepository.findById(UsersApplicationSupport.userId(query.userId()))
                .orElseThrow(() -> new UserManagementException(BusinessErrorType.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        return UsersApplicationSupport.toDetails(user);
    }
}

