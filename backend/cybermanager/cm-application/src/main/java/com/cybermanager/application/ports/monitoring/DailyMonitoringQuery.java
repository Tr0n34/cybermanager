package com.cybermanager.application.ports.monitoring;

import com.cybermanager.application.views.monitoring.DailyCustomerActivityView;
import com.cybermanager.application.views.monitoring.DailyCustomerView;

import java.util.List;
import java.util.UUID;

public interface DailyMonitoringQuery {
    List<DailyCustomerView> getDailyCustomers();
    DailyCustomerActivityView getDailyCustomerActivity(UUID customerId);
}
