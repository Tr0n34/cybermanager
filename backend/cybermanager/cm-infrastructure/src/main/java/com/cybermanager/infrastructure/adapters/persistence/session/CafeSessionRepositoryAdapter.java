package com.cybermanager.infrastructure.adapters.persistence.session;

import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.session.CafeSession;
import com.cybermanager.domain.model.session.SessionId;
import com.cybermanager.domain.port.session.CafeSessionRepository;
import com.cybermanager.infrastructure.entities.persistence.session.CafeSessionJpaEntity;
import com.cybermanager.infrastructure.repositories.persistence.session.CafeSessionJpaRepository;
import com.cybermanager.domain.model.shared.Money;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class CafeSessionRepositoryAdapter implements CafeSessionRepository {
    private final CafeSessionJpaRepository repository;

    public CafeSessionRepositoryAdapter(CafeSessionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public CafeSession save(CafeSession session) {
        CafeSessionJpaEntity entity = new CafeSessionJpaEntity();
        entity.id = session.id().value();
        entity.customerId = session.customerId().value();
        entity.workstation = session.workstation();
        entity.startedAt = session.startedAt();
        entity.endedAt = session.endedAt();
        entity.consumedMinutes = session.consumedMinutes();
        entity.calculatedPrice = session.calculatedPrice().amount();
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<CafeSession> findById(SessionId sessionId) {
        return repository.findById(sessionId.value()).map(this::toDomain);
    }

    @Override
    public List<CafeSession> findByDay(LocalDate date) {
        return repository.findByStartedAtBetween(date.atStartOfDay(), date.plusDays(1).atStartOfDay()).stream().map(this::toDomain).toList();
    }

    @Override
    public List<CafeSession> findActive() {
        return repository.findByEndedAtIsNull().stream().map(this::toDomain).toList();
    }

    private CafeSession toDomain(CafeSessionJpaEntity entity) {
        return new CafeSession(new SessionId(entity.id), new CustomerId(entity.customerId), entity.workstation, entity.startedAt, entity.endedAt, entity.consumedMinutes, new Money(entity.calculatedPrice == null ? java.math.BigDecimal.ZERO : entity.calculatedPrice));
    }
}

