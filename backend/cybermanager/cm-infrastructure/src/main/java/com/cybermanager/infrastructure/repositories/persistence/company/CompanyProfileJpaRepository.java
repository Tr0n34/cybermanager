package com.cybermanager.infrastructure.repositories.persistence.company;

import com.cybermanager.infrastructure.entities.persistence.company.CompanyProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyProfileJpaRepository extends JpaRepository<CompanyProfileJpaEntity, UUID> {
    Optional<CompanyProfileJpaEntity> findTopByOrderByUpdatedAtDesc();
}
