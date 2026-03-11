package com.cybermanager.application.views.reporting;

import java.time.LocalDate;
import java.util.List;

public record DayHistoryView(LocalDate date, List<DayCustomerHistoryView> customers) {
}
