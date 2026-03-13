package com.cybermanager.auth.application.services;

import com.cybermanager.auth.application.commands.AuthenticateUserCommand;
import com.cybermanager.auth.application.ports.PasswordVerifier;
import com.cybermanager.auth.application.ports.TokenIssuer;
import com.cybermanager.auth.application.usecases.AuthenticateUserUseCase;
import com.cybermanager.auth.application.views.AuthenticationResultView;
import com.cybermanager.auth.domain.model.EmailAddress;
import com.cybermanager.auth.domain.model.UserAccount;
import com.cybermanager.auth.domain.port.UserAuthenticationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AuthenticateUserService implements AuthenticateUserUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticateUserService.class);

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
        LOGGER.info("Authentication attempt email={}", command.email());
        UserAccount user = userAuthenticationRepository.findByEmail(new EmailAddress(command.email()))
                .orElseThrow(() -> {
                    LOGGER.warn("Authentication failed because user was not found email={}", command.email());
                    return new AuthenticationFailedException("Invalid credentials");
                });

        if (!user.isActive()) {
            LOGGER.warn("Authentication failed because user is disabled userId={} email={}", user.id().value(), user.email().value());
            throw new AuthenticationFailedException("User is disabled");
        }

        if (!passwordVerifier.matches(command.password(), user.passwordHash().value())) {
            LOGGER.warn("Authentication failed because password verification did not match userId={} email={}", user.id().value(), user.email().value());
            throw new AuthenticationFailedException("Invalid credentials");
        }

        LOGGER.info("Authentication succeeded userId={} email={} roles={}", user.id().value(), user.email().value(), user.roles().size());

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

