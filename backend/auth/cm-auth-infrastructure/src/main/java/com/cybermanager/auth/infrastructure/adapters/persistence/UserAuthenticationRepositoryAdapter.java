package com.cybermanager.auth.infrastructure.adapters.persistence;

import com.cybermanager.auth.domain.model.AppRole;
import com.cybermanager.auth.domain.model.EmailAddress;
import com.cybermanager.auth.domain.model.PasswordHash;
import com.cybermanager.auth.domain.model.UserAccount;
import com.cybermanager.auth.domain.model.UserId;
import com.cybermanager.auth.domain.model.UserStatus;
import com.cybermanager.auth.domain.port.UserAuthenticationRepository;
import com.cybermanager.auth.infrastructure.entities.persistence.UserJpaEntity;
import com.cybermanager.auth.infrastructure.repositories.persistence.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UserAuthenticationRepositoryAdapter implements UserAuthenticationRepository {
    private final UserJpaRepository userJpaRepository;

    public UserAuthenticationRepositoryAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Optional<UserAccount> findByEmail(EmailAddress email) {
        return userJpaRepository.findByEmail(email.value()).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findById(UserId id) {
        return userJpaRepository.findById(id.value()).map(this::toDomain);
    }

    private UserAccount toDomain(UserJpaEntity entity) {
        return new UserAccount(
                new UserId(entity.getId()),
                new EmailAddress(entity.getEmail()),
                entity.getFirstName(),
                entity.getLastName(),
                new PasswordHash(entity.getPasswordHash()),
                entity.getRoles().stream().map(role -> AppRole.valueOf(role.name())).collect(Collectors.toSet()),
                UserStatus.valueOf(entity.getStatus().name())
        );
    }
}

