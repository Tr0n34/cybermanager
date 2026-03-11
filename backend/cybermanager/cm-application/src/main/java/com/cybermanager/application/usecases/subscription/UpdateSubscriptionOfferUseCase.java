package com.cybermanager.application.usecases.subscription;

import com.cybermanager.application.commands.subscription.UpdateSubscriptionOfferCommand;
import com.cybermanager.application.views.subscription.SubscriptionOfferView;

public interface UpdateSubscriptionOfferUseCase {
    SubscriptionOfferView execute(UpdateSubscriptionOfferCommand command);
}
