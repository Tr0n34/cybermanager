package com.cybermanager.application.ports.reporting;

import com.cybermanager.application.views.reporting.CustomerDayHistoryView;
import com.cybermanager.application.views.reporting.DayHistoryView;

import java.time.LocalDate;
import java.util.UUID;

public interface DayHistoryQuery {
    DayHistoryView getDayHistory(LocalDate date);
    CustomerDayHistoryView getCustomerDayHistory(LocalDate date, UUID customerId);
}
