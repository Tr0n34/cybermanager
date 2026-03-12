package com.cybermanager.infrastructure.adapters.query.monitoring;

import com.cybermanager.infrastructure.repositories.persistence.customer.CustomerJpaRepository;
import com.cybermanager.application.ports.monitoring.DailyMonitoringQuery;
import com.cybermanager.application.views.monitoring.DailyCustomerActivityView;
import com.cybermanager.application.views.monitoring.DailyCustomerView;
import com.cybermanager.infrastructure.repositories.persistence.sales.SaleJpaRepository;
import com.cybermanager.infrastructure.repositories.persistence.session.CafeSessionJpaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class MonitoringQueryAdapter implements DailyMonitoringQuery {
    private final CustomerJpaRepository customerRepository;
    private final SaleJpaRepository saleRepository;
    private final CafeSessionJpaRepository sessionRepository;

    public MonitoringQueryAdapter(CustomerJpaRepository customerRepository, SaleJpaRepository saleRepository, CafeSessionJpaRepository sessionRepository) {
        this.customerRepository = customerRepository;
        this.saleRepository = saleRepository;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public List<DailyCustomerView> getDailyCustomers() {
        var todayStart = LocalDate.now().atStartOfDay();
        var todayEnd = LocalDate.now().plusDays(1).atStartOfDay();
        var sales = saleRepository.findBySoldAtBetween(todayStart, todayEnd);
        var sessions = sessionRepository.findByStartedAtBetween(todayStart, todayEnd);
        var activeIds = sessionRepository.findByEndedAtIsNull().stream().map(s -> s.customerId).collect(java.util.stream.Collectors.toSet());
        return customerRepository.findAll().stream().map(customer -> new DailyCustomerView(
                customer.id,
                customer.name,
                customer.type,
                customer.remainingMinutes,
                sessions.stream().filter(session -> session.customerId.equals(customer.id)).mapToInt(session -> session.consumedMinutes).sum(),
                sales.stream().filter(sale -> sale.customerId.equals(customer.id)).map(sale -> sale.totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                activeIds.contains(customer.id)
        )).toList();
    }

    @Override
    public DailyCustomerActivityView getDailyCustomerActivity(UUID customerId) {
        var todayStart = LocalDate.now().atStartOfDay();
        var todayEnd = LocalDate.now().plusDays(1).atStartOfDay();
        var customer = customerRepository.findById(customerId).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        var sales = saleRepository.findBySoldAtBetween(todayStart, todayEnd).stream()
                .filter(sale -> sale.customerId.equals(customerId))
                .map(sale -> sale.type + " - " + sale.totalAmount)
                .toList();
        var sessions = sessionRepository.findByStartedAtBetween(todayStart, todayEnd).stream()
                .filter(session -> session.customerId.equals(customerId))
                .map(session -> session.workstation + " - " + session.startedAt + " -> " + session.endedAt)
                .toList();
        return new DailyCustomerActivityView(customer.id, customer.name, sales, sessions);
    }
}

