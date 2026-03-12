package com.cybermanager.infrastructure.repositories.persistence.subscription;

import com.cybermanager.infrastructure.entities.persistence.subscription.SubscriptionOfferJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SubscriptionOfferJpaRepository extends JpaRepository<SubscriptionOfferJpaEntity, UUID> {
}

