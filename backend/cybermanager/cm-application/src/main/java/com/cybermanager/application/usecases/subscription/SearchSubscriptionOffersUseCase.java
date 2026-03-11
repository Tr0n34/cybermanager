package com.cybermanager.application.usecases.subscription;

import com.cybermanager.application.queries.subscription.SearchSubscriptionOffersQuery;
import com.cybermanager.application.views.subscription.SubscriptionOfferView;

import java.util.List;

public interface SearchSubscriptionOffersUseCase {
    List<SubscriptionOfferView> execute(SearchSubscriptionOffersQuery query);
}
