package com.cybermanager.infrastructure.adapters.persistence.sales;

import com.cybermanager.domain.model.sales.ConnectionPricingRule;
import com.cybermanager.domain.model.sales.ConnectionPricingTier;
import com.cybermanager.domain.port.sales.ConnectionPricingRepository;
import com.cybermanager.infrastructure.entities.persistence.sales.ConnectionPricingTierJpaEntity;
import com.cybermanager.infrastructure.repositories.persistence.sales.ConnectionPricingJpaRepository;
import com.cybermanager.domain.model.shared.Money;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConnectionPricingRepositoryAdapter implements ConnectionPricingRepository {
    private final ConnectionPricingJpaRepository repository;

    public ConnectionPricingRepositoryAdapter(ConnectionPricingJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ConnectionPricingRule save(ConnectionPricingRule rule) {
        repository.deleteAllInBatch();
        List<ConnectionPricingTierJpaEntity> entities = rule.tiers().stream()
                .map(tier -> {
                    ConnectionPricingTierJpaEntity entity = new ConnectionPricingTierJpaEntity();
                    entity.durationMinutes = tier.durationMinutes();
                    entity.price = tier.price().amount();
                    return entity;
                })
                .toList();
        repository.saveAll(entities);
        return rule;
    }

    @Override
    public ConnectionPricingRule getCurrentRule() {
        List<ConnectionPricingTier> tiers = repository.findAllByOrderByDurationMinutesAsc().stream()
                .map(entity -> new ConnectionPricingTier(entity.durationMinutes, new Money(entity.price)))
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
}

