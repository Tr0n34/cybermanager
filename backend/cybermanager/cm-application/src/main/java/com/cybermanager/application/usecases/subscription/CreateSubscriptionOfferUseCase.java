package com.cybermanager.application.usecases.subscription;

import com.cybermanager.application.commands.subscription.CreateSubscriptionOfferCommand;
import com.cybermanager.application.views.subscription.SubscriptionOfferView;

public interface CreateSubscriptionOfferUseCase {
    SubscriptionOfferView execute(CreateSubscriptionOfferCommand command);
}
