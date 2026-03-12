package com.cybermanager.infrastructure.repositories.persistence.users;

import com.cybermanager.infrastructure.entities.persistence.users.UserJpaEntity;
import com.cybermanager.infrastructure.entities.persistence.users.UserStatusJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    Optional<UserJpaEntity> findByEmail(String email);

    List<UserJpaEntity> findByStatus(UserStatusJpa status);
}

