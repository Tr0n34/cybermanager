package com.cybermanager.auth.application.services;

import com.cybermanager.auth.application.commands.AuthenticateUserCommand;
import com.cybermanager.auth.application.ports.PasswordVerifier;
import com.cybermanager.auth.application.ports.TokenIssuer;
import com.cybermanager.auth.application.usecases.AuthenticateUserUseCase;
import com.cybermanager.auth.application.views.AuthenticationResultView;
import com.cybermanager.auth.domain.model.EmailAddress;
import com.cybermanager.auth.domain.model.UserAccount;
import com.cybermanager.auth.domain.port.UserAuthenticationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AuthenticateUserService implements AuthenticateUserUseCase {
    private final UserAuthenticationRepository userAuthenticationRepository;
    private final PasswordVerifier passwordVerifier;
    private final TokenIssuer tokenIssuer;

    public AuthenticateUserService(
            UserAuthenticationRepository userAuthenticationRepository,
            PasswordVerifier passwordVerifier,
            TokenIssuer tokenIssuer
    ) {
        this.userAuthenticationRepository = userAuthenticationRepository;
        this.passwordVerifier = passwordVerifier;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public AuthenticationResultView execute(AuthenticateUserCommand command) {
        UserAccount user = userAuthenticationRepository.findByEmail(new EmailAddress(command.email()))
                .orElseThrow(() -> new AuthenticationFailedException("Invalid credentials"));

        if (!user.isActive()) {
            throw new AuthenticationFailedException("User is disabled");
        }

        if (!passwordVerifier.matches(command.password(), user.passwordHash().value())) {
            throw new AuthenticationFailedException("Invalid credentials");
        }

        return new AuthenticationResultView(
                tokenIssuer.issue(user),
                user.id().value(),
                user.email().value(),
                user.firstName(),
                user.lastName(),
                user.roles().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }
}

