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
        entity.pausedAt = session.pausedAt();
        entity.paid = session.paid();
        entity.pausedSeconds = session.pausedSeconds();
        entity.pausedMinutes = session.pausedSeconds() / 60;
        entity.consumedSeconds = session.consumedSeconds();
        entity.consumedMinutes = (session.consumedSeconds() + 59) / 60;
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
        int pausedMinutes = entity.pausedMinutes == null ? 0 : entity.pausedMinutes;
        int consumedMinutes = entity.consumedMinutes == null ? 0 : entity.consumedMinutes;
        int pausedSeconds = entity.pausedSeconds != null && entity.pausedSeconds > 0 ? entity.pausedSeconds : pausedMinutes * 60;
        int consumedSeconds = entity.consumedSeconds != null && entity.consumedSeconds > 0 ? entity.consumedSeconds : consumedMinutes * 60;
        return new CafeSession(
                new SessionId(entity.id),
                new CustomerId(entity.customerId),
                entity.workstation,
                entity.startedAt,
                entity.endedAt,
                entity.pausedAt,
                Boolean.TRUE.equals(entity.paid),
                pausedSeconds,
                consumedSeconds,
                new Money(entity.calculatedPrice == null ? java.math.BigDecimal.ZERO : entity.calculatedPrice)
        );
    }
}

