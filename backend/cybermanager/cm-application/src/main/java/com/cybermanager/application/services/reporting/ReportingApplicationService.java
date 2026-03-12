package com.cybermanager.application.services.reporting;

import com.cybermanager.application.ports.reporting.DayHistoryQuery;
import com.cybermanager.application.views.reporting.CustomerDayHistoryView;
import com.cybermanager.application.views.reporting.DayHistoryView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReportingApplicationService {
    private final DayHistoryQuery queryAdapter;

    public ReportingApplicationService(DayHistoryQuery queryAdapter) {
        this.queryAdapter = queryAdapter;
    }

    public DayHistoryView dayHistory(LocalDate date) {
        return queryAdapter.getDayHistory(date);
    }

    public CustomerDayHistoryView customerHistory(LocalDate date, UUID customerId) {
        return queryAdapter.getCustomerDayHistory(date, customerId);
    }
}

