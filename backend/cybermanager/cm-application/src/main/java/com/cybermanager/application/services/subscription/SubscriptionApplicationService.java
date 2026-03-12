package com.cybermanager.application.services.subscription;

import com.cybermanager.application.commands.subscription.ActivateSubscriptionOfferCommand;
import com.cybermanager.application.commands.subscription.CreateSubscriptionOfferCommand;
import com.cybermanager.application.commands.subscription.DeactivateSubscriptionOfferCommand;
import com.cybermanager.application.commands.subscription.DeleteSubscriptionOfferCommand;
import com.cybermanager.application.commands.subscription.UpdateSubscriptionOfferCommand;
import com.cybermanager.application.queries.subscription.GetSubscriptionOfferDetailsQuery;
import com.cybermanager.application.queries.subscription.SearchSubscriptionOffersQuery;
import com.cybermanager.application.services.shared.ActorSupport;
import com.cybermanager.application.usecases.subscription.ActivateSubscriptionOfferUseCase;
import com.cybermanager.application.usecases.subscription.CreateSubscriptionOfferUseCase;
import com.cybermanager.application.usecases.subscription.DeactivateSubscriptionOfferUseCase;
import com.cybermanager.application.usecases.subscription.DeleteSubscriptionOfferUseCase;
import com.cybermanager.application.usecases.subscription.GetSubscriptionOfferDetailsUseCase;
import com.cybermanager.application.usecases.subscription.SearchSubscriptionOffersUseCase;
import com.cybermanager.application.usecases.subscription.UpdateSubscriptionOfferUseCase;
import com.cybermanager.application.views.subscription.SubscriptionOfferView;
import com.cybermanager.domain.model.shared.Money;
import com.cybermanager.domain.model.subscription.SubscriptionOffer;
import com.cybermanager.domain.model.subscription.SubscriptionOfferId;
import com.cybermanager.domain.model.subscription.SubscriptionOfferStatus;
import com.cybermanager.domain.port.subscription.SubscriptionOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SubscriptionApplicationService implements
        CreateSubscriptionOfferUseCase,
        UpdateSubscriptionOfferUseCase,
        ActivateSubscriptionOfferUseCase,
        DeactivateSubscriptionOfferUseCase,
        DeleteSubscriptionOfferUseCase,
        SearchSubscriptionOffersUseCase,
        GetSubscriptionOfferDetailsUseCase {
    private final SubscriptionOfferRepository repository;

    public SubscriptionApplicationService(SubscriptionOfferRepository repository) {
        this.repository = repository;
    }

    public SubscriptionOfferView execute(CreateSubscriptionOfferCommand command) {
        ActorSupport.requireAdmin(command.actorRoles());
        return toView(repository.save(SubscriptionOffer.create(command.name(), new Money(command.price()), command.includedMinutes())));
    }

    @Override
    public SubscriptionOfferView execute(UpdateSubscriptionOfferCommand command) {
        ActorSupport.requireAdmin(command.actorRoles());
        var offer = repository.findById(new SubscriptionOfferId(command.offerId()))
                .orElseThrow(() -> new IllegalArgumentException("Subscription offer not found"));
        return toView(repository.save(offer.update(command.name(), new Money(command.price()), command.includedMinutes())));
    }

    @Override
    public SubscriptionOfferView execute(ActivateSubscriptionOfferCommand command) {
        ActorSupport.requireAdmin(command.actorRoles());
        var offer = repository.findById(new SubscriptionOfferId(command.offerId()))
                .orElseThrow(() -> new IllegalArgumentException("Subscription offer not found"));
        return toView(repository.save(offer.activate()));
    }

    @Override
    public SubscriptionOfferView execute(DeactivateSubscriptionOfferCommand command) {
        ActorSupport.requireAdmin(command.actorRoles());
        var offer = repository.findById(new SubscriptionOfferId(command.offerId()))
                .orElseThrow(() -> new IllegalArgumentException("Subscription offer not found"));
        return toView(repository.save(offer.deactivate()));
    }

    @Override
    public void execute(DeleteSubscriptionOfferCommand command) {
        ActorSupport.requireAdmin(command.actorRoles());
        var offerId = new SubscriptionOfferId(command.offerId());
        repository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription offer not found"));
        repository.deleteById(offerId);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionOfferView> execute(SearchSubscriptionOffersQuery query) {
        SubscriptionOfferStatus status = query.status() == null || query.status().isBlank() ? null : SubscriptionOfferStatus.valueOf(query.status().toUpperCase());
        return repository.search(query.term(), status).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionOfferView execute(GetSubscriptionOfferDetailsQuery query) {
        return repository.findById(new SubscriptionOfferId(query.offerId())).map(this::toView)
                .orElseThrow(() -> new IllegalArgumentException("Subscription offer not found"));
    }

    private SubscriptionOfferView toView(SubscriptionOffer offer) {
        return new SubscriptionOfferView(offer.id().value(), offer.name(), offer.price().amount(), offer.includedMinutes(), offer.status().name());
    }
}

