package com.cybermanager.application.services.monitoring;

import com.cybermanager.application.ports.monitoring.DailyMonitoringQuery;
import com.cybermanager.application.views.monitoring.DailyCustomerActivityView;
import com.cybermanager.application.views.monitoring.DailyCustomerView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MonitoringApplicationService {
    private final DailyMonitoringQuery queryAdapter;

    public MonitoringApplicationService(DailyMonitoringQuery queryAdapter) {
        this.queryAdapter = queryAdapter;
    }

    public List<DailyCustomerView> customers() {
        return queryAdapter.getDailyCustomers();
    }

    public DailyCustomerActivityView customerActivity(UUID customerId) {
        return queryAdapter.getDailyCustomerActivity(customerId);
    }
}

