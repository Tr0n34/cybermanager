package com.cybermanager.infrastructure.entities.persistence.sales;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "cm_sale_lines")
public class SaleLineJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String label;
    public int quantity;
    public BigDecimal unitPrice;
    public BigDecimal totalPrice;

    @ManyToOne
    @JoinColumn(name = "sale_id")
    public SaleJpaEntity sale;
}

