package com.cybermanager.application.services.users;

import com.cybermanager.application.commands.users.CreateUserCommand;
import com.cybermanager.application.ports.users.PasswordHasher;
import com.cybermanager.application.services.shared.BusinessErrorType;
import com.cybermanager.application.usecases.users.CreateUserUseCase;
import com.cybermanager.application.views.users.UserDetailsView;
import com.cybermanager.domain.model.users.User;
import com.cybermanager.domain.model.users.UserId;
import com.cybermanager.domain.port.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateUserService implements CreateUserUseCase {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public CreateUserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public UserDetailsView execute(CreateUserCommand command) {
        UsersApplicationSupport.requireAdmin(command.actorRoles());
        var email = UsersApplicationSupport.email(command.email());
        if (userRepository.existsByEmail(email)) {
            throw new UserManagementException(BusinessErrorType.CONFLICT, "USER_EMAIL_EXISTS", "Email already exists");
        }

        User user = User.create(
                UserId.newId(),
                email,
                command.firstName(),
                command.lastName(),
                UsersApplicationSupport.passwordHash(passwordHasher.hash(command.password())),
                UsersApplicationSupport.mapRoles(command.roles())
        );

        return UsersApplicationSupport.toDetails(userRepository.save(user));
    }
}

