package com.cybermanager.application.views.monitoring;

import java.util.List;
import java.util.UUID;

public record DailyCustomerActivityView(UUID customerId, String name, List<String> sales, List<String> sessions) {
}
