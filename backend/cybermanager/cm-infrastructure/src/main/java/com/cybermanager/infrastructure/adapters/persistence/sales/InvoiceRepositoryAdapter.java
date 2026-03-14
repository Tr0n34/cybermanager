package com.cybermanager.infrastructure.adapters.persistence.sales;

import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.sales.Invoice;
import com.cybermanager.domain.model.sales.InvoiceLine;
import com.cybermanager.domain.model.sales.InvoiceStatus;
import com.cybermanager.domain.model.shared.Money;
import com.cybermanager.domain.port.sales.InvoiceRepository;
import com.cybermanager.infrastructure.entities.persistence.sales.InvoiceJpaEntity;
import com.cybermanager.infrastructure.repositories.persistence.sales.InvoiceJpaRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class InvoiceRepositoryAdapter implements InvoiceRepository {
    private final InvoiceJpaRepository repository;

    public InvoiceRepositoryAdapter(InvoiceJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceJpaEntity entity = new InvoiceJpaEntity();
        entity.id = invoice.id();
        entity.saleId = invoice.saleId();
        entity.customerId = invoice.customerId() == null ? null : invoice.customerId().value();
        entity.invoiceNumber = invoice.invoiceNumber();
        entity.customerName = invoice.customerName();
        entity.issuedAt = invoice.issuedAt();
        entity.status = invoice.status().name();
        entity.totalAmount = invoice.totalAmount().amount();
        entity.lines = invoice.lines().stream().map(line -> {
            var lineEntity = new com.cybermanager.infrastructure.entities.persistence.sales.InvoiceLineJpaEntity();
            lineEntity.invoice = entity;
            lineEntity.label = line.label();
            lineEntity.quantity = line.quantity();
            lineEntity.unitPrice = line.unitPrice().amount();
            lineEntity.totalPrice = line.totalPrice().amount();
            return lineEntity;
        }).toList();
        return toDomain(repository.save(entity));
    }

    @Override
    public List<Invoice> search(String invoiceNumber, String customerName, InvoiceStatus status, LocalDate startDate, LocalDate endDate) {
        Specification<InvoiceJpaEntity> specification = Specification.where(null);
        if (invoiceNumber != null && !invoiceNumber.isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("invoiceNumber")), "%" + invoiceNumber.toLowerCase() + "%"));
        }
        if (customerName != null && !customerName.isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("customerName")), "%" + customerName.toLowerCase() + "%"));
        }
        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status.name()));
        }
        if (startDate != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("issuedAt"), startDate.atStartOfDay()));
        }
        if (endDate != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.lessThan(root.get("issuedAt"), endDate.plusDays(1).atStartOfDay()));
        }
        return repository.findAll(specification).stream()
                .sorted(Comparator.comparing((InvoiceJpaEntity entity) -> entity.issuedAt).reversed())
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Invoice> findById(UUID invoiceId) {
        return repository.findById(invoiceId).map(this::toDomain);
    }

    private Invoice toDomain(InvoiceJpaEntity entity) {
        return new Invoice(
                entity.id,
                entity.saleId,
                entity.invoiceNumber,
                entity.customerId == null ? null : new CustomerId(entity.customerId),
                entity.customerName,
                entity.issuedAt,
                InvoiceStatus.valueOf(entity.status),
                new Money(entity.totalAmount),
                entity.lines.stream()
                        .map(line -> new InvoiceLine(line.label, line.quantity, new Money(line.unitPrice), new Money(line.totalPrice)))
                        .toList()
        );
    }
}
