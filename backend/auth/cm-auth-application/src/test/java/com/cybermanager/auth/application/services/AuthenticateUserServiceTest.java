package com.cybermanager.auth.application.services;

import com.cybermanager.auth.application.commands.AuthenticateUserCommand;
import com.cybermanager.auth.application.ports.PasswordVerifier;
import com.cybermanager.auth.application.ports.TokenIssuer;
import com.cybermanager.auth.domain.model.AppRole;
import com.cybermanager.auth.domain.model.EmailAddress;
import com.cybermanager.auth.domain.model.PasswordHash;
import com.cybermanager.auth.domain.model.UserAccount;
import com.cybermanager.auth.domain.model.UserId;
import com.cybermanager.auth.domain.model.UserStatus;
import com.cybermanager.auth.domain.port.UserAuthenticationRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticateUserServiceTest {
    @Test
    void shouldAuthenticateActiveUser() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        PasswordVerifier passwordVerifier = mock(PasswordVerifier.class);
        TokenIssuer tokenIssuer = mock(TokenIssuer.class);
        AuthenticateUserService service = new AuthenticateUserService(repository, passwordVerifier, tokenIssuer);

        UserAccount user = new UserAccount(
                new UserId(UUID.randomUUID()),
                new EmailAddress("admin@cybermanager.local"),
                "Admin",
                "Manager",
                new PasswordHash("hash"),
                Set.of(AppRole.ADMIN),
                UserStatus.ACTIVE
        );

        when(repository.findByEmail(any())).thenReturn(Optional.of(user));
        when(passwordVerifier.matches("admin123", "hash")).thenReturn(true);
        when(tokenIssuer.issue(user)).thenReturn("jwt-token");

        var result = service.execute(new AuthenticateUserCommand("admin@cybermanager.local", "admin123"));

        assertEquals("jwt-token", result.token());
        assertEquals("admin@cybermanager.local", result.email());
    }

    @Test
    void shouldRejectDisabledUser() {
        UserAuthenticationRepository repository = mock(UserAuthenticationRepository.class);
        PasswordVerifier passwordVerifier = mock(PasswordVerifier.class);
        TokenIssuer tokenIssuer = mock(TokenIssuer.class);
        AuthenticateUserService service = new AuthenticateUserService(repository, passwordVerifier, tokenIssuer);

        UserAccount user = new UserAccount(
                new UserId(UUID.randomUUID()),
                new EmailAddress("disabled@cybermanager.local"),
                "Disabled",
                "User",
                new PasswordHash("hash"),
                Set.of(AppRole.EMPLOYEE),
                UserStatus.DISABLED
        );

        when(repository.findByEmail(any())).thenReturn(Optional.of(user));

        assertThrows(
                AuthenticationFailedException.class,
                () -> service.execute(new AuthenticateUserCommand("disabled@cybermanager.local", "pwd"))
        );
    }
}

