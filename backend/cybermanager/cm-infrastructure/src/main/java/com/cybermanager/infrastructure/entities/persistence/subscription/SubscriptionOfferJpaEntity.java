package com.cybermanager.infrastructure.entities.persistence.subscription;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cm_subscription_offers")
public class SubscriptionOfferJpaEntity {
    @Id
    public UUID id;
    public String name;
    public BigDecimal price;
    public int includedMinutes;
    public String status;
}

