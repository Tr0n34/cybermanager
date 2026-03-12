package com.cybermanager.application.services.users;

import com.cybermanager.application.commands.users.ChangeUserStatusCommand;
import com.cybermanager.application.services.shared.BusinessErrorType;
import com.cybermanager.application.usecases.users.DisableUserUseCase;
import com.cybermanager.application.views.users.UserDetailsView;
import com.cybermanager.domain.port.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DisableUserService implements DisableUserUseCase {
    private final UserRepository userRepository;

    public DisableUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetailsView execute(ChangeUserStatusCommand command) {
        UsersApplicationSupport.requireAdmin(command.actorRoles());
        var user = userRepository.findById(UsersApplicationSupport.userId(command.userId()))
                .orElseThrow(() -> new UserManagementException(BusinessErrorType.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        return UsersApplicationSupport.toDetails(userRepository.save(user.disable()));
    }
}

