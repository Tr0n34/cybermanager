package com.cybermanager.infrastructure.adapters.query.monitoring;

import com.cybermanager.application.ports.monitoring.DailyMonitoringQuery;
import com.cybermanager.application.views.monitoring.DailyCustomerActivityView;
import com.cybermanager.application.views.monitoring.DailyCustomerView;
import com.cybermanager.infrastructure.entities.persistence.customer.DebtJpaEntity;
import com.cybermanager.infrastructure.entities.persistence.sales.SaleJpaEntity;
import com.cybermanager.infrastructure.entities.persistence.sales.SaleLineJpaEntity;
import com.cybermanager.infrastructure.entities.persistence.session.CafeSessionJpaEntity;
import com.cybermanager.infrastructure.repositories.persistence.customer.CustomerJpaRepository;
import com.cybermanager.infrastructure.repositories.persistence.customer.DebtJpaRepository;
import com.cybermanager.infrastructure.repositories.persistence.sales.SaleJpaRepository;
import com.cybermanager.infrastructure.repositories.persistence.session.CafeSessionJpaRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class MonitoringQueryAdapter implements DailyMonitoringQuery {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CustomerJpaRepository customerRepository;
    private final SaleJpaRepository saleRepository;
    private final CafeSessionJpaRepository sessionRepository;
    private final DebtJpaRepository debtRepository;

    public MonitoringQueryAdapter(
            CustomerJpaRepository customerRepository,
            SaleJpaRepository saleRepository,
            CafeSessionJpaRepository sessionRepository,
            DebtJpaRepository debtRepository
    ) {
        this.customerRepository = customerRepository;
        this.saleRepository = saleRepository;
        this.sessionRepository = sessionRepository;
        this.debtRepository = debtRepository;
    }

    @Override
    public List<DailyCustomerView> getDailyCustomers() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        var sales = saleRepository.findBySoldAtBetween(start, end);
        var sessions = sessionRepository.findByStartedAtBetween(start, end);
        var openDebts = debtRepository.findByStatus("OPEN");

        return customerRepository.findAll().stream()
                .map(customer -> {
                    var customerSessions = sessions.stream().filter(session -> session.customerId.equals(customer.id)).toList();
                    var purchasesTotal = sales.stream()
                            .filter(sale -> sale.customerId.equals(customer.id))
                            .map(sale -> sale.totalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    var debtTotal = openDebts.stream()
                            .filter(debt -> debt.customerId.equals(customer.id))
                            .map(debt -> debt.amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new DailyCustomerView(
                            customer.id,
                            customer.name,
                            "SUBSCRIBER".equals(customer.type) ? "Abonne" : "Client",
                            customer.remainingMinutes,
                            customerSessions.stream().mapToInt(session -> session.consumedMinutes == null ? 0 : session.consumedMinutes).sum(),
                            purchasesTotal,
                            debtTotal,
                            purchasesTotal,
                            resolveSessionState(customerSessions)
                    );
                })
                .filter(item -> item.purchasesTotal().compareTo(BigDecimal.ZERO) > 0
                        || item.consumedMinutes() > 0
                        || item.debtTotal().compareTo(BigDecimal.ZERO) > 0
                        || !"Terminee".equals(item.sessionState()))
                .sorted(Comparator.comparing(DailyCustomerView::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public DailyCustomerActivityView getDailyCustomerActivity(UUID customerId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return buildActivity(customerId, start, end);
    }

    private DailyCustomerActivityView buildActivity(UUID customerId, LocalDateTime start, LocalDateTime end) {
        var customer = customerRepository.findById(customerId).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        var sales = saleRepository.findBySoldAtBetween(start, end).stream()
                .filter(sale -> sale.customerId.equals(customerId))
                .toList();
        var sessions = sessionRepository.findByStartedAtBetween(start, end).stream()
                .filter(session -> session.customerId.equals(customerId))
                .toList();
        var debts = debtRepository.findByStatus("OPEN").stream()
                .filter(debt -> debt.customerId.equals(customerId))
                .toList();

        BigDecimal totalDebtCreated = debts.stream()
                .map(debt -> debt.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal salesTotal = sales.stream()
                .map(sale -> sale.totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCollected = salesTotal;

        return new DailyCustomerActivityView(
                customer.id,
                customer.name,
                sales.stream()
                        .flatMap(sale -> sale.lines.stream())
                        .map(this::toSaleActivity)
                        .toList(),
                sessions.stream().flatMap(session -> sessionEvents(session).stream()).toList(),
                totalCollected,
                totalDebtCreated
        );
    }

    private DailyCustomerActivityView.SaleActivityView toSaleActivity(SaleLineJpaEntity line) {
        return new DailyCustomerActivityView.SaleActivityView(line.label, line.quantity, line.totalPrice);
    }

    private List<String> sessionEvents(CafeSessionJpaEntity session) {
        var events = new java.util.ArrayList<String>();
        events.add("Session demarree le " + format(session.startedAt));
        if (session.pausedAt != null && session.endedAt == null) {
            events.add("Session en pause depuis le " + format(session.pausedAt));
        }
        if (session.endedAt != null) {
            events.add("Session arretee le " + format(session.endedAt));
        }
        return events;
    }

    private String resolveSessionState(List<CafeSessionJpaEntity> sessions) {
        return sessions.stream()
                .max(Comparator.comparing(session -> session.startedAt))
                .map(session -> {
                    if (session.endedAt == null && session.pausedAt != null) {
                        return "Pause";
                    }
                    if (session.endedAt == null) {
                        return "En cours";
                    }
                    return Boolean.TRUE.equals(session.paid) ? "Paye" : "Terminee";
                })
                .orElse("Terminee");
    }

    private String format(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
    }
}
