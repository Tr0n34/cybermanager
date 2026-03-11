package com.cybermanager.application.views.reporting;

import java.util.List;
import java.util.UUID;

public record CustomerDayHistoryView(UUID customerId, String name, List<String> sales, List<String> sessions) {
}
