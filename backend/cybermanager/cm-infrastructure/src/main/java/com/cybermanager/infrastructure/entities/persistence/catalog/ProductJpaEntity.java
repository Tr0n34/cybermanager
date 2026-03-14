package com.cybermanager.infrastructure.entities.persistence.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cm_products")
public class ProductJpaEntity {
    @Id
    public UUID id;
    public String name;
    public BigDecimal price;
    public String category;
    public String description;
    public String status;
}

