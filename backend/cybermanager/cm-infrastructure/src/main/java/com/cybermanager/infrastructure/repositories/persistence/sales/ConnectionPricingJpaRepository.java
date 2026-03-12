package com.cybermanager.infrastructure.repositories.persistence.sales;

import com.cybermanager.infrastructure.entities.persistence.sales.ConnectionPricingTierJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConnectionPricingJpaRepository extends JpaRepository<ConnectionPricingTierJpaEntity, Long> {
    List<ConnectionPricingTierJpaEntity> findAllByOrderByDurationMinutesAsc();
}

