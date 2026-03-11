package com.cybermanager.application.usecases.sales;

import com.cybermanager.application.commands.sales.ConfigureConnectionPricingCommand;
import com.cybermanager.application.views.sales.ConnectionPricingView;

public interface ConfigureConnectionPricingUseCase {
    ConnectionPricingView execute(ConfigureConnectionPricingCommand command);
}
