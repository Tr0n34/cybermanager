package com.cybermanager.infrastructure.repositories.persistence.sales;

import com.cybermanager.infrastructure.entities.persistence.sales.SaleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SaleJpaRepository extends JpaRepository<SaleJpaEntity, UUID> {
    List<SaleJpaEntity> findBySoldAtBetween(LocalDateTime start, LocalDateTime end);
    List<SaleJpaEntity> findByCustomerIdOrderBySoldAtDesc(UUID customerId);
}
