package com.cybermanager.infrastructure.repositories.persistence.sales;

import com.cybermanager.infrastructure.entities.persistence.sales.InvoiceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface InvoiceJpaRepository extends JpaRepository<InvoiceJpaEntity, UUID>, JpaSpecificationExecutor<InvoiceJpaEntity> {
}
