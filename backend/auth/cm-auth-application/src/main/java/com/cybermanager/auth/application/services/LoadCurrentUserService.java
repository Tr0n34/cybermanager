package com.cybermanager.auth.application.services;

import com.cybermanager.auth.application.ports.TokenReader;
import com.cybermanager.auth.application.queries.LoadCurrentUserQuery;
import com.cybermanager.auth.application.usecases.LoadCurrentUserUseCase;
import com.cybermanager.auth.application.views.CurrentUserView;
import com.cybermanager.auth.domain.model.UserId;
import com.cybermanager.auth.domain.port.UserAuthenticationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LoadCurrentUserService implements LoadCurrentUserUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadCurrentUserService.class);

    private final TokenReader tokenReader;
    private final UserAuthenticationRepository userAuthenticationRepository;

    public LoadCurrentUserService(TokenReader tokenReader, UserAuthenticationRepository userAuthenticationRepository) {
        this.tokenReader = tokenReader;
        this.userAuthenticationRepository = userAuthenticationRepository;
    }

    @Override
    public CurrentUserView execute(LoadCurrentUserQuery query) {
        LOGGER.debug("Loading current user from bearer token");
        var userId = new UserId(tokenReader.readUserId(query.token()));
        var user = userAuthenticationRepository.findById(userId)
                .orElseThrow(() -> {
                    LOGGER.warn("Current user lookup failed because the token user was not found userId={}", userId.value());
                    return new AuthenticationFailedException("Current user not found");
                });
        if (!user.isActive()) {
            LOGGER.warn("Current user lookup failed because user is disabled userId={}", user.id().value());
            throw new AuthenticationFailedException("User is disabled");
        }
        LOGGER.debug("Current user loaded userId={} roles={}", user.id().value(), user.roles().size());
        return new CurrentUserView(
                user.id().value(),
                user.email().value(),
                user.firstName(),
                user.lastName(),
                user.roles().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }
}

