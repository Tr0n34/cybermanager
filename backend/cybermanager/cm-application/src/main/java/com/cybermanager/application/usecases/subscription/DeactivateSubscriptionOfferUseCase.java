package com.cybermanager.application.usecases.subscription;

import com.cybermanager.application.commands.subscription.DeactivateSubscriptionOfferCommand;
import com.cybermanager.application.views.subscription.SubscriptionOfferView;

public interface DeactivateSubscriptionOfferUseCase {
    SubscriptionOfferView execute(DeactivateSubscriptionOfferCommand command);
}
