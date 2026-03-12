package com.cybermanager.infrastructure.entities.persistence.session;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cm_sessions")
public class CafeSessionJpaEntity {
    @Id
    public UUID id;
    public UUID customerId;
    public String workstation;
    public LocalDateTime startedAt;
    public LocalDateTime endedAt;
    public LocalDateTime pausedAt;
    public Boolean paid;
    public Integer pausedMinutes;
    public Integer pausedSeconds;
    public Integer consumedMinutes;
    public Integer consumedSeconds;
    public BigDecimal calculatedPrice;
}

