package com.cybermanager.infrastructure.repositories.persistence.customer;

import com.cybermanager.infrastructure.entities.persistence.customer.GeneratedArchiveFileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GeneratedArchiveFileJpaRepository extends JpaRepository<GeneratedArchiveFileJpaEntity, UUID> {
    List<GeneratedArchiveFileJpaEntity> findAllByOrderByGeneratedAtDesc();
}
