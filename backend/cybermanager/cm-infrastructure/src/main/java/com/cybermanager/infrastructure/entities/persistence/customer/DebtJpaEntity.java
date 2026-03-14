package com.cybermanager.infrastructure.entities.persistence.customer;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cm_customer_debts")
public class DebtJpaEntity {
    @Id
    public UUID id;
    public UUID customerId;
    public String label;
    public String comment;
    public BigDecimal amount;
    public String status;
    public LocalDateTime createdAt;
    public LocalDateTime settledAt;
}
