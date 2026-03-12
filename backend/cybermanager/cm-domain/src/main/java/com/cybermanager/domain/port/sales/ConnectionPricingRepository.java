package com.cybermanager.domain.port.sales;

import com.cybermanager.domain.model.sales.ConnectionPricingRule;

public interface ConnectionPricingRepository {
    ConnectionPricingRule save(ConnectionPricingRule rule);
    ConnectionPricingRule getCurrentRule();
}

