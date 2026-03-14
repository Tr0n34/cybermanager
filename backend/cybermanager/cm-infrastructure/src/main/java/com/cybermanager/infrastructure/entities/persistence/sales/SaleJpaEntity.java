package com.cybermanager.infrastructure.entities.persistence.sales;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cm_sales")
public class SaleJpaEntity {
    @Id
    public UUID id;
    public UUID customerId;
    public UUID sessionId;
    public String type;
    public LocalDateTime soldAt;
    public BigDecimal totalAmount;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<SaleLineJpaEntity> lines = new ArrayList<>();
}

