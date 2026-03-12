package com.cybermanager.infrastructure.repositories.persistence.catalog;

import com.cybermanager.infrastructure.entities.persistence.catalog.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {
}

