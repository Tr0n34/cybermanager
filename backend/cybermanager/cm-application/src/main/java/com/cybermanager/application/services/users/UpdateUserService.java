package com.cybermanager.application.services.users;

import com.cybermanager.application.commands.users.UpdateUserCommand;
import com.cybermanager.application.ports.users.PasswordHasher;
import com.cybermanager.application.services.shared.BusinessErrorType;
import com.cybermanager.application.usecases.users.UpdateUserUseCase;
import com.cybermanager.application.views.users.UserDetailsView;
import com.cybermanager.domain.model.users.User;
import com.cybermanager.domain.port.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateUserService implements UpdateUserUseCase {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UpdateUserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public UserDetailsView execute(UpdateUserCommand command) {
        UsersApplicationSupport.requireAdmin(command.actorRoles());
        var userId = UsersApplicationSupport.userId(command.userId());
        User current = userRepository.findById(userId)
                .orElseThrow(() -> new UserManagementException(BusinessErrorType.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        var email = UsersApplicationSupport.email(command.email());
        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new UserManagementException(BusinessErrorType.CONFLICT, "USER_EMAIL_EXISTS", "Email already exists");
        }
        var passwordHash = command.password() == null || command.password().isBlank()
                ? null
                : UsersApplicationSupport.passwordHash(passwordHasher.hash(command.password()));

        User updated = current.update(
                email,
                command.firstName(),
                command.lastName(),
                UsersApplicationSupport.mapRoles(command.roles()),
                passwordHash
        );
        return UsersApplicationSupport.toDetails(userRepository.save(updated));
    }
}

