package com.cybermanager.application.services.session;

import com.cybermanager.application.commands.session.StartSessionCommand;
import com.cybermanager.application.commands.session.StopSessionCommand;
import com.cybermanager.application.queries.session.SearchSessionsOfDayQuery;
import com.cybermanager.application.services.shared.DateTimeLabelFormatter;
import com.cybermanager.application.services.shared.BusinessErrorType;
import com.cybermanager.application.services.shared.BusinessException;
import com.cybermanager.application.usecases.session.GetCurrentSessionsUseCase;
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
import com.cybermanager.domain.port.session.CafeSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SessionApplicationService implements
        StartSessionUseCase,
        StopSessionUseCase,
        SearchSessionsOfDayUseCase,
        GetCurrentSessionsUseCase {
    private static final String DEFAULT_WORKSTATION = "SESSION";

    private final CafeSessionRepository sessionRepository;
    private final CustomerRepository customerRepository;
    private final ConnectionPricingRepository connectionPricingRepository;
    private final DebtRepository debtRepository;

    public SessionApplicationService(
            CafeSessionRepository sessionRepository,
            CustomerRepository customerRepository,
            ConnectionPricingRepository connectionPricingRepository,
            DebtRepository debtRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.customerRepository = customerRepository;
        this.connectionPricingRepository = connectionPricingRepository;
        this.debtRepository = debtRepository;
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

        Money price = Money.of("0");
        if (customer.type() == CustomerType.WALK_IN) {
            ConnectionPricingRule pricing = connectionPricingRepository.getCurrentRule();
            int minutes = (int) Math.max(1, java.time.Duration.between(session.startedAt(), LocalDateTime.now()).toMinutes());
            price = pricing.priceForMinutes(minutes);
            if (price.amount().signum() > 0) {
                debtRepository.save(DebtRecord.create(customer.id(), "Session du " + DateTimeLabelFormatter.format(session.startedAt()), price, LocalDateTime.now()));
            }
        } else {
            int consumedMinutes = (int) Math.max(1, java.time.Duration.between(session.startedAt(), LocalDateTime.now()).toMinutes());
            customer = customerRepository.save(customer.deductMinutes(consumedMinutes));
        }

        return toView(sessionRepository.save(session.stop(LocalDateTime.now(), price)), customer);
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
        return new SessionView(
                session.id().value(),
                customer.id().value(),
                customer.name(),
                customer.type().name(),
                customer.remainingMinutes(),
                session.workstation(),
                session.startedAt(),
                session.endedAt(),
                session.consumedMinutes(),
                session.calculatedPrice().amount()
        );
    }
}
