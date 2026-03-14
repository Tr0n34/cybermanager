package com.cybermanager.infrastructure.entities.persistence.sales;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "cm_invoices",
        indexes = {
                @Index(name = "idx_cm_invoices_number", columnList = "invoiceNumber"),
                @Index(name = "idx_cm_invoices_customer_issued_at", columnList = "customerName, issuedAt"),
                @Index(name = "idx_cm_invoices_status_issued_at", columnList = "status, issuedAt")
        }
)
public class InvoiceJpaEntity {
    @Id
    public UUID id;
    public UUID saleId;
    public UUID customerId;
    public String invoiceNumber;
    public String customerName;
    public LocalDateTime issuedAt;
    public String status;
    public BigDecimal totalAmount;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<InvoiceLineJpaEntity> lines = new ArrayList<>();
}
