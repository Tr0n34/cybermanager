package com.cybermanager.application.usecases.subscription;

import com.cybermanager.application.commands.subscription.DeleteSubscriptionOfferCommand;

public interface DeleteSubscriptionOfferUseCase {
    void execute(DeleteSubscriptionOfferCommand command);
}
