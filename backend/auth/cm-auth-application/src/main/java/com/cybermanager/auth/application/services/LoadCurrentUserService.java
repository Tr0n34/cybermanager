package com.cybermanager.auth.application.services;

import com.cybermanager.auth.application.ports.TokenReader;
import com.cybermanager.auth.application.queries.LoadCurrentUserQuery;
import com.cybermanager.auth.application.usecases.LoadCurrentUserUseCase;
import com.cybermanager.auth.application.views.CurrentUserView;
import com.cybermanager.auth.domain.model.UserId;
import com.cybermanager.auth.domain.port.UserAuthenticationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LoadCurrentUserService implements LoadCurrentUserUseCase {
    private final TokenReader tokenReader;
    private final UserAuthenticationRepository userAuthenticationRepository;

    public LoadCurrentUserService(TokenReader tokenReader, UserAuthenticationRepository userAuthenticationRepository) {
        this.tokenReader = tokenReader;
        this.userAuthenticationRepository = userAuthenticationRepository;
    }

    @Override
    public CurrentUserView execute(LoadCurrentUserQuery query) {
        var userId = new UserId(tokenReader.readUserId(query.token()));
        var user = userAuthenticationRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("Current user not found"));
        if (!user.isActive()) {
            throw new AuthenticationFailedException("User is disabled");
        }
        return new CurrentUserView(
                user.id().value(),
                user.email().value(),
                user.firstName(),
                user.lastName(),
                user.roles().stream().map(Enum::name).collect(Collectors.toSet())
        );
    }
}

