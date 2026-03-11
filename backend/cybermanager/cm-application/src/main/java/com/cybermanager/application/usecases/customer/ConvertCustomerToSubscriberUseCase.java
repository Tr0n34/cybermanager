package com.cybermanager.application.usecases.customer;

import com.cybermanager.application.commands.customer.ConvertCustomerToSubscriberCommand;
import com.cybermanager.application.views.customer.ConversionView;

public interface ConvertCustomerToSubscriberUseCase {
    ConversionView execute(ConvertCustomerToSubscriberCommand command);
}
