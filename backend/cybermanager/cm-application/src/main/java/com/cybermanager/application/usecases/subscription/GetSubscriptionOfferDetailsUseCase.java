package com.cybermanager.application.usecases.subscription;

import com.cybermanager.application.queries.subscription.GetSubscriptionOfferDetailsQuery;
import com.cybermanager.application.views.subscription.SubscriptionOfferView;

public interface GetSubscriptionOfferDetailsUseCase {
    SubscriptionOfferView execute(GetSubscriptionOfferDetailsQuery query);
}
