package com.cybermanager.application.services.session;

import com.cybermanager.application.commands.session.PauseSessionCommand;
import com.cybermanager.application.commands.session.ResumeSessionCommand;
import com.cybermanager.application.commands.session.StartSessionCommand;
import com.cybermanager.application.commands.session.StopSessionCommand;
import com.cybermanager.application.queries.session.SearchSessionsOfDayQuery;
import com.cybermanager.application.services.shared.DateTimeLabelFormatter;
import com.cybermanager.application.services.shared.BusinessErrorType;
import com.cybermanager.application.services.shared.BusinessException;
import com.cybermanager.application.usecases.session.GetCurrentSessionsUseCase;
import com.cybermanager.application.usecases.session.PauseSessionUseCase;
import com.cybermanager.application.usecases.session.ResumeSessionUseCase;
import com.cybermanager.application.usecases.session.SearchSessionsOfDayUseCase;
import com.cybermanager.application.usecases.session.StartSessionUseCase;
import com.cybermanager.application.usecases.session.StopSessionUseCase;
import com.cybermanager.application.views.session.CurrentSessionsView;
import com.cybermanager.application.views.session.SessionView;
import com.cybermanager.domain.model.customer.Customer;
import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.customer.CustomerStatus;
import com.cybermanager.domain.model.customer.CustomerType;
import com.cybermanager.domain.model.customer.DebtRecord;
import com.cybermanager.domain.model.sales.ConnectionPricingRule;
import com.cybermanager.domain.model.session.CafeSession;
import com.cybermanager.domain.model.session.SessionId;
import com.cybermanager.domain.model.shared.Money;
import com.cybermanager.domain.port.customer.CustomerRepository;
import com.cybermanager.domain.port.customer.DebtRepository;
import com.cybermanager.domain.port.sales.ConnectionPricingRepository;
import com.cybermanager.domain.port.sales.SaleRepository;
import com.cybermanager.domain.port.session.CafeSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class SessionApplicationService implements
        StartSessionUseCase,
        StopSessionUseCase,
        PauseSessionUseCase,
        ResumeSessionUseCase,
        SearchSessionsOfDayUseCase,
        GetCurrentSessionsUseCase {
    private static final String DEFAULT_WORKSTATION = "SESSION";

    private final CafeSessionRepository sessionRepository;
    private final CustomerRepository customerRepository;
    private final ConnectionPricingRepository connectionPricingRepository;
    private final DebtRepository debtRepository;
    private final SaleRepository saleRepository;

    public SessionApplicationService(
            CafeSessionRepository sessionRepository,
            CustomerRepository customerRepository,
            ConnectionPricingRepository connectionPricingRepository,
            DebtRepository debtRepository,
            SaleRepository saleRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.customerRepository = customerRepository;
        this.connectionPricingRepository = connectionPricingRepository;
        this.debtRepository = debtRepository;
        this.saleRepository = saleRepository;
    }

    @Override
    public SessionView execute(StartSessionCommand command) {
        Customer customer;
        if (command.customerId() != null) {
            customer = customerRepository.findById(new CustomerId(command.customerId()))
                    .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
            if (customer.type() != CustomerType.SUBSCRIBER) {
                throw new BusinessException(BusinessErrorType.VALIDATION, "SESSION_SUBSCRIBER_REQUIRED", "Only subscribers can be searched and reused");
            }
        } else if (command.customerName() != null && !command.customerName().isBlank()) {
            customer = customerRepository.save(Customer.createWalkIn(command.customerName().trim()));
        } else {
            throw new BusinessException(BusinessErrorType.VALIDATION, "SESSION_CUSTOMER_REQUIRED", "Customer name or subscriber is required");
        }

        if (customer.status() != CustomerStatus.ACTIVE) {
            throw new BusinessException(BusinessErrorType.FORBIDDEN, "CUSTOMER_INACTIVE", "Customer is not active");
        }
        if (customer.type() == CustomerType.SUBSCRIBER && customer.remainingMinutes() <= 0) {
            throw new BusinessException(BusinessErrorType.VALIDATION, "SUBSCRIBER_NO_REMAINING_MINUTES", "Subscriber has no remaining minutes");
        }

        var session = sessionRepository.save(CafeSession.start(customer.id(), DEFAULT_WORKSTATION, LocalDateTime.now()));
        return toView(session, customer);
    }

    @Override
    public SessionView execute(StopSessionCommand command) {
        var session = sessionRepository.findById(new SessionId(command.sessionId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found"));
        var customer = customerRepository.findById(session.customerId())
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));

        LocalDateTime stoppedAt = LocalDateTime.now();
        Money price = Money.of("0");
        if (customer.type() == CustomerType.WALK_IN) {
            ConnectionPricingRule pricing = connectionPricingRepository.getCurrentRule();
            int minutes = session.consumedMinutesUntil(stoppedAt);
            price = pricing.priceForMinutes(minutes);
            if (!command.paid() && price.amount().signum() > 0) {
                debtRepository.save(DebtRecord.create(customer.id(), "Session du " + DateTimeLabelFormatter.format(session.startedAt()), price, stoppedAt));
            }
        } else {
            int consumedMinutes = session.consumedMinutesUntil(stoppedAt);
            customer = customerRepository.save(customer.deductMinutes(consumedMinutes));
        }

        return toView(sessionRepository.save(session.stop(stoppedAt, price, command.paid())), customer);
    }

    @Override
    public SessionView execute(PauseSessionCommand command) {
        var session = sessionRepository.findById(new SessionId(command.sessionId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found"));
        if (session.paused()) {
            throw new BusinessException(BusinessErrorType.CONFLICT, "SESSION_ALREADY_PAUSED", "Session is already paused");
        }
        var customer = customerRepository.findById(session.customerId())
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
        return toView(sessionRepository.save(session.pause(LocalDateTime.now())), customer);
    }

    @Override
    public SessionView execute(ResumeSessionCommand command) {
        var session = sessionRepository.findById(new SessionId(command.sessionId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found"));
        if (!session.paused()) {
            throw new BusinessException(BusinessErrorType.CONFLICT, "SESSION_NOT_PAUSED", "Session is not paused");
        }
        var customer = customerRepository.findById(session.customerId())
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
        return toView(sessionRepository.save(session.resume(LocalDateTime.now())), customer);
    }

    @Transactional(readOnly = true)
    @Override
    public List<SessionView> execute(SearchSessionsOfDayQuery query) {
        return sessionRepository.findByDay(query.date() == null ? LocalDate.now() : query.date()).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public CurrentSessionsView execute() {
        return new CurrentSessionsView(sessionRepository.findActive().stream().map(this::toView).toList());
    }

    private SessionView toView(CafeSession session) {
        var customer = customerRepository.findById(session.customerId())
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
        return toView(session, customer);
    }

    private SessionView toView(CafeSession session, Customer customer) {
        LocalDateTime referenceTime = session.endedAt() == null ? LocalDateTime.now() : session.endedAt();
        int consumedSeconds = session.endedAt() == null ? session.consumedSecondsUntil(referenceTime) : session.consumedSeconds();
        int consumedMinutes = session.endedAt() == null ? session.consumedMinutesUntil(referenceTime) : Math.max(0, (session.consumedSeconds() + 59) / 60);
        Money connectionAmount = session.calculatedPrice();
        if (session.endedAt() == null && customer.type() == CustomerType.WALK_IN) {
            connectionAmount = connectionPricingRepository.getCurrentRule().priceForMinutes(consumedMinutes);
        }
        var salesOfDay = saleRepository.findByCustomerId(customer.id()).stream()
                .filter(sale -> sale.soldAt().toLocalDate().equals(referenceTime.toLocalDate()))
                .toList();
        Money purchasesAmount = salesOfDay.stream()
                .map(sale -> sale.totalAmount())
                .reduce(Money.of("0"), Money::add);
        var allOpenDebts = debtRepository.findByCustomerId(customer.id()).stream()
                .filter(debt -> debt.status() == com.cybermanager.domain.model.customer.DebtStatus.OPEN)
                .toList();
        var openDebtsOfDay = allOpenDebts.stream()
                .filter(debt -> debt.createdAt().toLocalDate().equals(referenceTime.toLocalDate()))
                .toList();
        Money openDebtAmount = allOpenDebts.stream()
                .map(com.cybermanager.domain.model.customer.DebtRecord::amount)
                .reduce(Money.of("0"), Money::add);
        BigDecimal openSalesDebtAmount = openDebtsOfDay.stream()
                .filter(debt -> debt.label().startsWith("Vente "))
                .map(debt -> debt.amount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidPurchasesAmount = purchasesAmount.amount().subtract(openSalesDebtAmount).max(BigDecimal.ZERO);
        BigDecimal dueConnectionAmount = session.paid() ? BigDecimal.ZERO : connectionAmount.amount();
        BigDecimal paidConnectionAmount = session.paid() ? connectionAmount.amount() : BigDecimal.ZERO;
        Money totalAmountDue = new Money(dueConnectionAmount.add(openSalesDebtAmount));
        Money totalPaidAmount = new Money(paidPurchasesAmount.add(paidConnectionAmount));
        return new SessionView(
                session.id().value(),
                customer.id().value(),
                customer.name(),
                customer.type().name(),
                customer.remainingMinutes(),
                session.workstation(),
                session.startedAt(),
                session.endedAt(),
                session.paused(),
                session.paid() && openDebtAmount.amount().compareTo(BigDecimal.ZERO) == 0,
                consumedSeconds,
                consumedMinutes,
                connectionAmount.amount(),
                purchasesAmount.amount(),
                openDebtAmount.amount(),
                totalAmountDue.amount(),
                totalPaidAmount.amount()
        );
    }
}
