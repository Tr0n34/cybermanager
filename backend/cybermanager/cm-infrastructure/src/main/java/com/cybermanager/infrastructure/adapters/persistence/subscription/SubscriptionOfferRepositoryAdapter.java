package com.cybermanager.infrastructure.adapters.persistence.subscription;

import com.cybermanager.domain.model.shared.Money;
import com.cybermanager.domain.model.subscription.SubscriptionOffer;
import com.cybermanager.domain.model.subscription.SubscriptionOfferId;
import com.cybermanager.domain.model.subscription.SubscriptionOfferStatus;
import com.cybermanager.domain.port.subscription.SubscriptionOfferRepository;
import com.cybermanager.infrastructure.entities.persistence.subscription.SubscriptionOfferJpaEntity;
import com.cybermanager.infrastructure.repositories.persistence.subscription.SubscriptionOfferJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SubscriptionOfferRepositoryAdapter implements SubscriptionOfferRepository {
    private final SubscriptionOfferJpaRepository repository;

    public SubscriptionOfferRepositoryAdapter(SubscriptionOfferJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public SubscriptionOffer save(SubscriptionOffer offer) {
        SubscriptionOfferJpaEntity entity = new SubscriptionOfferJpaEntity();
        entity.id = offer.id().value();
        entity.name = offer.name();
        entity.price = offer.price().amount();
        entity.includedMinutes = offer.includedMinutes();
        entity.status = offer.status().name();
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<SubscriptionOffer> findById(SubscriptionOfferId offerId) {
        return repository.findById(offerId.value()).map(this::toDomain);
    }

    @Override
    public void deleteById(SubscriptionOfferId offerId) {
        repository.deleteById(offerId.value());
    }

    @Override
    public List<SubscriptionOffer> search(String term, SubscriptionOfferStatus status) {
        String search = term == null ? "" : term.toLowerCase();
        return repository.findAll().stream()
                .filter(item -> search.isBlank() || item.name.toLowerCase().contains(search))
                .filter(item -> status == null || item.status.equals(status.name()))
                .map(this::toDomain)
                .toList();
    }

    private SubscriptionOffer toDomain(SubscriptionOfferJpaEntity entity) {
        return new SubscriptionOffer(new SubscriptionOfferId(entity.id), entity.name, new Money(entity.price), entity.includedMinutes, SubscriptionOfferStatus.valueOf(entity.status));
    }
}

