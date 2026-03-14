package com.cybermanager.application.queries.customer;

import java.time.LocalDate;

public record SearchCustomerArchiveCandidatesQuery(LocalDate startDate, LocalDate endDate, String type) {
}
