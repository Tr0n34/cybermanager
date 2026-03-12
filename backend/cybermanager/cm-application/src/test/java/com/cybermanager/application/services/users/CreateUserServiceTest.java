package com.cybermanager.application.services.users;

import com.cybermanager.application.commands.users.CreateUserCommand;
import com.cybermanager.application.ports.users.PasswordHasher;
import com.cybermanager.domain.model.users.EmailAddress;
import com.cybermanager.domain.model.users.User;
import com.cybermanager.domain.port.users.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreateUserServiceTest {
    @Test
    void shouldCreateUserForAdmin() {
        UserRepository repository = mock(UserRepository.class);
        PasswordHasher passwordHasher = mock(PasswordHasher.class);
        CreateUserService service = new CreateUserService(repository, passwordHasher);

        when(repository.existsByEmail(any(EmailAddress.class))).thenReturn(false);
        when(passwordHasher.hash("password")).thenReturn("hashed");
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.execute(new CreateUserCommand(
                "admin@cybermanager.local",
                Set.of("ADMIN"),
                "employee@cybermanager.local",
                "Alice",
                "Martin",
                "password",
                Set.of("EMPLOYEE")
        ));

        assertEquals("employee@cybermanager.local", result.email());
        assertEquals("ACTIVE", result.status());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        UserRepository repository = mock(UserRepository.class);
        PasswordHasher passwordHasher = mock(PasswordHasher.class);
        CreateUserService service = new CreateUserService(repository, passwordHasher);

        when(repository.existsByEmail(any(EmailAddress.class))).thenReturn(true);

        assertThrows(UserManagementException.class, () -> service.execute(new CreateUserCommand(
                "admin@cybermanager.local",
                Set.of("ADMIN"),
                "employee@cybermanager.local",
                "Alice",
                "Martin",
                "password",
                Set.of("EMPLOYEE")
        )));
    }
}

