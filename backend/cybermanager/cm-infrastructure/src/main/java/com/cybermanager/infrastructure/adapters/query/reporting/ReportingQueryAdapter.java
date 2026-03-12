package com.cybermanager.infrastructure.adapters.query.reporting;

import com.cybermanager.infrastructure.repositories.persistence.customer.CustomerJpaRepository;
import com.cybermanager.application.ports.reporting.DayHistoryQuery;
import com.cybermanager.application.views.reporting.CustomerDayHistoryView;
import com.cybermanager.application.views.reporting.DayCustomerHistoryView;
import com.cybermanager.application.views.reporting.DayHistoryView;
import com.cybermanager.infrastructure.repositories.persistence.sales.SaleJpaRepository;
import com.cybermanager.infrastructure.repositories.persistence.session.CafeSessionJpaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class ReportingQueryAdapter implements DayHistoryQuery {
    private final CustomerJpaRepository customerRepository;
    private final SaleJpaRepository saleRepository;
    private final CafeSessionJpaRepository sessionRepository;

    public ReportingQueryAdapter(CustomerJpaRepository customerRepository, SaleJpaRepository saleRepository, CafeSessionJpaRepository sessionRepository) {
        this.customerRepository = customerRepository;
        this.saleRepository = saleRepository;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public DayHistoryView getDayHistory(LocalDate date) {
        var start = date.atStartOfDay();
        var end = date.plusDays(1).atStartOfDay();
        var sales = saleRepository.findBySoldAtBetween(start, end);
        var sessions = sessionRepository.findByStartedAtBetween(start, end);
        var customers = customerRepository.findAll().stream().map(customer -> new DayCustomerHistoryView(
                customer.id,
                customer.name,
                customer.type,
                sessions.stream().filter(session -> session.customerId.equals(customer.id)).mapToInt(session -> session.consumedMinutes).sum(),
                sales.stream().filter(sale -> sale.customerId.equals(customer.id)).map(sale -> sale.totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
        )).toList();
        return new DayHistoryView(date, customers);
    }

    @Override
    public CustomerDayHistoryView getCustomerDayHistory(LocalDate date, UUID customerId) {
        var start = date.atStartOfDay();
        var end = date.plusDays(1).atStartOfDay();
        var customer = customerRepository.findById(customerId).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        var sales = saleRepository.findBySoldAtBetween(start, end).stream()
                .filter(sale -> sale.customerId.equals(customerId))
                .map(sale -> sale.type + " - " + sale.totalAmount)
                .toList();
        var sessions = sessionRepository.findByStartedAtBetween(start, end).stream()
                .filter(session -> session.customerId.equals(customerId))
                .map(session -> session.workstation + " - " + session.startedAt + " -> " + session.endedAt)
                .toList();
        return new CustomerDayHistoryView(customer.id, customer.name, sales, sessions);
    }
}

