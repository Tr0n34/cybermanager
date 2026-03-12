package com.cybermanager.application.services.users;

import com.cybermanager.application.commands.users.DeleteUserCommand;
import com.cybermanager.application.services.shared.BusinessErrorType;
import com.cybermanager.application.usecases.users.DeleteUserUseCase;
import com.cybermanager.domain.port.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteUserService implements DeleteUserUseCase {
    private final UserRepository userRepository;

    public DeleteUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void execute(DeleteUserCommand command) {
        UsersApplicationSupport.requireAdmin(command.actorRoles());
        var userId = UsersApplicationSupport.userId(command.userId());
        userRepository.findById(userId)
                .orElseThrow(() -> new UserManagementException(BusinessErrorType.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        userRepository.deleteById(userId);
    }
}
