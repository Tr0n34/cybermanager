package com.cybermanager.infrastructure.adapters.persistence.sales;

import com.cybermanager.domain.model.sales.ConnectionPricingRule;
import com.cybermanager.domain.model.sales.ConnectionPricingTier;
import com.cybermanager.domain.port.sales.ConnectionPricingRepository;
import com.cybermanager.infrastructure.entities.persistence.sales.ConnectionPricingTierJpaEntity;
import com.cybermanager.infrastructure.repositories.persistence.sales.ConnectionPricingJpaRepository;
import com.cybermanager.domain.model.shared.Money;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ConnectionPricingRepositoryAdapter implements ConnectionPricingRepository {
    private final ConnectionPricingJpaRepository repository;

    public ConnectionPricingRepositoryAdapter(ConnectionPricingJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ConnectionPricingRule save(ConnectionPricingRule rule) {
        List<ConnectionPricingTierJpaEntity> existingEntities = repository.findAllByOrderByDurationMinutesAsc();
        var existingByDuration = existingEntities.stream()
                .collect(Collectors.toMap(entity -> entity.durationMinutes, Function.identity()));
        LocalDateTime now = LocalDateTime.now();

        List<ConnectionPricingTierJpaEntity> entitiesToSave = rule.tiers().stream()
                .map(tier -> {
                    ConnectionPricingTierJpaEntity existing = existingByDuration.remove(tier.durationMinutes());
                    ConnectionPricingTierJpaEntity entity = existing == null ? new ConnectionPricingTierJpaEntity() : existing;
                    entity.durationMinutes = tier.durationMinutes();
                    entity.price = tier.price().amount();
                    entity.createdAt = existing == null ? now : (existing.createdAt == null ? now : existing.createdAt);
                    entity.updatedAt = now;
                    return entity;
                })
                .toList();

        if (!existingByDuration.isEmpty()) {
          repository.deleteAll(existingByDuration.values());
        }

        List<ConnectionPricingTier> savedTiers = repository.saveAll(entitiesToSave).stream()
                .map(this::toDomain)
                .toList();
        return new ConnectionPricingRule(savedTiers);
    }

    @Override
    public ConnectionPricingRule getCurrentRule() {
        List<ConnectionPricingTier> tiers = repository.findAllByOrderByDurationMinutesAsc().stream()
                .map(this::toDomain)
                .toList();
        if (tiers.isEmpty()) {
            return new ConnectionPricingRule(List.of(
                    new ConnectionPricingTier(30, Money.of("1.50")),
                    new ConnectionPricingTier(60, Money.of("2.50")),
                    new ConnectionPricingTier(120, Money.of("4.50"))
            ));
        }
        return new ConnectionPricingRule(tiers);
    }

    private ConnectionPricingTier toDomain(ConnectionPricingTierJpaEntity entity) {
        LocalDateTime createdAt = entity.createdAt == null ? entity.updatedAt : entity.createdAt;
        LocalDateTime updatedAt = entity.updatedAt == null ? createdAt : entity.updatedAt;
        return new ConnectionPricingTier(entity.id, entity.durationMinutes, new Money(entity.price), createdAt, updatedAt);
    }
}

