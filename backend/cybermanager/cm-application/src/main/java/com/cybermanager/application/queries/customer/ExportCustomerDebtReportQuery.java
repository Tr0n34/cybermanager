package com.cybermanager.application.queries.customer;

import java.time.LocalDate;

public record ExportCustomerDebtReportQuery(String actorEmail, LocalDate startDate, LocalDate endDate, String type) {
}
