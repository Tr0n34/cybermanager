package com.cybermanager.infrastructure.repositories.persistence.customer;

import com.cybermanager.infrastructure.entities.persistence.customer.DebtJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DebtJpaRepository extends JpaRepository<DebtJpaEntity, UUID> {
    List<DebtJpaEntity> findByCustomerId(UUID customerId);
    List<DebtJpaEntity> findByStatus(String status);
}
