package com.cybermanager.application.usecases.subscription;

import com.cybermanager.application.commands.subscription.ActivateSubscriptionOfferCommand;
import com.cybermanager.application.views.subscription.SubscriptionOfferView;

public interface ActivateSubscriptionOfferUseCase {
    SubscriptionOfferView execute(ActivateSubscriptionOfferCommand command);
}
