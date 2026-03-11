package com.cybermanager.infrastructure.adapters.persistence.sales;

import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.sales.Sale;
import com.cybermanager.domain.model.sales.SaleId;
import com.cybermanager.domain.model.sales.SaleLine;
import com.cybermanager.domain.model.sales.SaleType;
import com.cybermanager.domain.model.shared.Money;
import com.cybermanager.domain.port.sales.SaleRepository;
import com.cybermanager.infrastructure.entities.persistence.sales.SaleJpaEntity;
import com.cybermanager.infrastructure.entities.persistence.sales.SaleLineJpaEntity;
import com.cybermanager.infrastructure.repositories.persistence.sales.SaleJpaRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class SaleRepositoryAdapter implements SaleRepository {
    private final SaleJpaRepository repository;

    public SaleRepositoryAdapter(SaleJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Sale save(Sale sale) {
        SaleJpaEntity entity = new SaleJpaEntity();
        entity.id = sale.id().value();
        entity.customerId = sale.customerId().value();
        entity.type = sale.type().name();
        entity.soldAt = sale.soldAt();
        entity.totalAmount = sale.totalAmount().amount();
        entity.lines = sale.lines().stream().map(line -> {
            SaleLineJpaEntity lineEntity = new SaleLineJpaEntity();
            lineEntity.sale = entity;
            lineEntity.label = line.label();
            lineEntity.quantity = line.quantity();
            lineEntity.unitPrice = line.unitPrice().amount();
            lineEntity.totalPrice = line.totalPrice().amount();
            return lineEntity;
        }).toList();
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Sale> findById(SaleId saleId) {
        return repository.findById(saleId.value()).map(this::toDomain);
    }

    @Override
    public List<Sale> findByDay(LocalDate date) {
        return repository.findBySoldAtBetween(date.atStartOfDay(), date.plusDays(1).atStartOfDay()).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Sale> findByCustomerId(CustomerId customerId) {
        return repository.findByCustomerIdOrderBySoldAtDesc(customerId.value()).stream().map(this::toDomain).toList();
    }

    private Sale toDomain(SaleJpaEntity entity) {
        return new Sale(
                new SaleId(entity.id),
                new CustomerId(entity.customerId),
                SaleType.valueOf(entity.type),
                entity.soldAt,
                entity.lines.stream().map(line -> new SaleLine(line.label, line.quantity, new Money(line.unitPrice), new Money(line.totalPrice))).toList(),
                new Money(entity.totalAmount)
        );
    }
}
