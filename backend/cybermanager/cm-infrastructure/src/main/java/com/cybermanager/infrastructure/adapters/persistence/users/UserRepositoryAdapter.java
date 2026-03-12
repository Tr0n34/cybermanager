package com.cybermanager.infrastructure.adapters.persistence.users;

import com.cybermanager.domain.model.users.AppRole;
import com.cybermanager.domain.model.users.EmailAddress;
import com.cybermanager.domain.model.users.PasswordHash;
import com.cybermanager.domain.model.users.User;
import com.cybermanager.domain.model.users.UserId;
import com.cybermanager.domain.model.users.UserStatus;
import com.cybermanager.domain.port.users.UserRepository;
import com.cybermanager.infrastructure.entities.persistence.users.AppRoleJpa;
import com.cybermanager.infrastructure.entities.persistence.users.UserJpaEntity;
import com.cybermanager.infrastructure.entities.persistence.users.UserStatusJpa;
import com.cybermanager.infrastructure.repositories.persistence.users.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository userJpaRepository;

    public UserRepositoryAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public User save(User user) {
        return toDomain(userJpaRepository.save(toEntity(user)));
    }

    @Override
    public Optional<User> findById(UserId id) {
        return userJpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public void deleteById(UserId id) {
        userJpaRepository.deleteById(id.value());
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return userJpaRepository.existsByEmail(email.value());
    }

    @Override
    public boolean existsByEmailAndIdNot(EmailAddress email, UserId userId) {
        return userJpaRepository.existsByEmailAndIdNot(email.value(), userId.value());
    }

    @Override
    public List<User> search(String term, UserStatus status) {
        String normalizedTerm = term == null ? "" : term.trim().toLowerCase();
        List<UserJpaEntity> source = status == null
                ? userJpaRepository.findAll()
                : userJpaRepository.findByStatus(UserStatusJpa.valueOf(status.name()));
        return source.stream()
                .filter(entity -> normalizedTerm.isBlank()
                        || entity.getEmail().toLowerCase().contains(normalizedTerm)
                        || entity.getFirstName().toLowerCase().contains(normalizedTerm)
                        || entity.getLastName().toLowerCase().contains(normalizedTerm))
                .map(this::toDomain)
                .toList();
    }

    private UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.id().value());
        entity.setEmail(user.email().value());
        entity.setFirstName(user.firstName());
        entity.setLastName(user.lastName());
        entity.setPasswordHash(user.passwordHash().value());
        entity.setStatus(UserStatusJpa.valueOf(user.status().name()));
        entity.setRoles(user.roles().stream().map(role -> AppRoleJpa.valueOf(role.name())).collect(Collectors.toSet()));
        return entity;
    }

    private User toDomain(UserJpaEntity entity) {
        return User.rehydrate(
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

