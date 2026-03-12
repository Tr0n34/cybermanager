package com.cybermanager.infrastructure.adapters.persistence.customer;

import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.customer.DebtId;
import com.cybermanager.domain.model.customer.DebtRecord;
import com.cybermanager.domain.model.customer.DebtStatus;
import com.cybermanager.domain.model.shared.Money;
import com.cybermanager.domain.port.customer.DebtRepository;
import com.cybermanager.infrastructure.entities.persistence.customer.DebtJpaEntity;
import com.cybermanager.infrastructure.repositories.persistence.customer.DebtJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DebtRepositoryAdapter implements DebtRepository {
    private final DebtJpaRepository repository;

    public DebtRepositoryAdapter(DebtJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public DebtRecord save(DebtRecord debtRecord) {
        DebtJpaEntity entity = new DebtJpaEntity();
        entity.id = debtRecord.id().value();
        entity.customerId = debtRecord.customerId().value();
        entity.label = debtRecord.label();
        entity.amount = debtRecord.amount().amount();
        entity.status = debtRecord.status().name();
        entity.createdAt = debtRecord.createdAt();
        entity.settledAt = debtRecord.settledAt();
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<DebtRecord> findById(DebtId debtId) {
        return repository.findById(debtId.value()).map(this::toDomain);
    }

    @Override
    public List<DebtRecord> findByCustomerId(CustomerId customerId) {
        return repository.findByCustomerId(customerId.value()).stream().map(this::toDomain).toList();
    }

    @Override
    public List<DebtRecord> findOpenDebts() {
        return repository.findByStatus(DebtStatus.OPEN.name()).stream().map(this::toDomain).toList();
    }

    private DebtRecord toDomain(DebtJpaEntity entity) {
        return new DebtRecord(
                new DebtId(entity.id),
                new CustomerId(entity.customerId),
                entity.label,
                new Money(entity.amount),
                DebtStatus.valueOf(entity.status),
                entity.createdAt,
                entity.settledAt
        );
    }
}
