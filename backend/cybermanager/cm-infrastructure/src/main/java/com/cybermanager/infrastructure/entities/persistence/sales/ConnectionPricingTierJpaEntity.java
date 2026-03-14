package com.cybermanager.infrastructure.entities.persistence.sales;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cm_connection_pricing_tier")
public class ConnectionPricingTierJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "duration_minutes", nullable = false)
    public int durationMinutes;

    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal price;

    @Column
    public LocalDateTime createdAt;

    @Column
    public LocalDateTime updatedAt;
}
