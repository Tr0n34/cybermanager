package com.cybermanager.domain.port.subscription;

import com.cybermanager.domain.model.subscription.SubscriptionOffer;
import com.cybermanager.domain.model.subscription.SubscriptionOfferId;
import com.cybermanager.domain.model.subscription.SubscriptionOfferStatus;

import java.util.List;
import java.util.Optional;

public interface SubscriptionOfferRepository {
    SubscriptionOffer save(SubscriptionOffer offer);
    Optional<SubscriptionOffer> findById(SubscriptionOfferId offerId);
    void deleteById(SubscriptionOfferId offerId);
    List<SubscriptionOffer> search(String term, SubscriptionOfferStatus status);
}

