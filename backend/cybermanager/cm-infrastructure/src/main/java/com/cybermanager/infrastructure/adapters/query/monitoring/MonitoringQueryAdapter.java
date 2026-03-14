package com.cybermanager.infrastructure.adapters.query.monitoring;

import com.cybermanager.application.ports.monitoring.DailyMonitoringQuery;
import com.cybermanager.application.services.shared.DateTimeLabelFormatter;
import com.cybermanager.application.views.monitoring.DailyCustomerActivityView;
import com.cybermanager.application.views.monitoring.DailyCustomerView;
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
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class MonitoringQueryAdapter implements DailyMonitoringQuery {
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
                            purchasesTotal.subtract(debtTotal),
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
        Set<String> debtLabels = debts.stream()
                .map(debt -> debt.label)
                .collect(Collectors.toSet());

        BigDecimal totalDebtCreated = debts.stream()
                .map(debt -> debt.amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal salesTotal = sales.stream()
                .map(sale -> sale.totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCollected = salesTotal.subtract(totalDebtCreated);

        return new DailyCustomerActivityView(
                customer.id,
                customer.name,
                sales.stream()
                        .sorted(Comparator.comparing((SaleJpaEntity sale) -> sale.soldAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .flatMap(sale -> sale.lines.stream().map(line -> toSaleActivity(sale, line, debtLabels.contains(debtLabelForSale(sale)))))
                        .toList(),
                sessions.stream()
                        .sorted(Comparator.comparing((CafeSessionJpaEntity session) -> session.startedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                        .map(this::toSessionActivity)
                        .toList(),
                totalCollected,
                totalDebtCreated
        );
    }

    private DailyCustomerActivityView.SaleActivityView toSaleActivity(SaleJpaEntity sale, SaleLineJpaEntity line, boolean debt) {
        return new DailyCustomerActivityView.SaleActivityView(line.label, line.quantity, line.totalPrice, debt, sale.soldAt);
    }

    private DailyCustomerActivityView.SessionActivityView toSessionActivity(CafeSessionJpaEntity session) {
        return new DailyCustomerActivityView.SessionActivityView(
                "Session du %s au %s".formatted(format(session.startedAt), format(session.endedAt)),
                session.startedAt,
                session.endedAt
        );
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
        return value == null ? "-" : DateTimeLabelFormatter.format(value);
    }

    private String debtLabelForSale(SaleJpaEntity sale) {
        return switch (sale.type) {
            case "PRODUCTS" -> sale.lines.stream()
                    .map(line -> line.quantity + " x " + line.label)
                    .reduce((left, right) -> left + ", " + right)
                    .map(label -> label + " du " + format(sale.soldAt))
                    .orElse("Vente produits du " + format(sale.soldAt));
            case "SUBSCRIPTION" -> "Vente abonnement du " + format(sale.soldAt);
            case "CONNECTION_TIME" -> "Vente temps du " + format(sale.soldAt);
            default -> "Vente du " + format(sale.soldAt);
        };
    }
}
