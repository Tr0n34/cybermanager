package com.cybermanager.application.services.session;

import com.cybermanager.application.commands.session.PauseSessionCommand;
import com.cybermanager.application.commands.session.PaySessionCommand;
import com.cybermanager.application.commands.session.RestartSessionsDayCommand;
import com.cybermanager.application.commands.session.ResumeSessionCommand;
import com.cybermanager.application.commands.session.StartSessionCommand;
import com.cybermanager.application.commands.session.StopSessionCommand;
import com.cybermanager.application.queries.session.SearchSessionsOfDayQuery;
import com.cybermanager.application.services.shared.DateTimeLabelFormatter;
import com.cybermanager.application.services.shared.BusinessErrorType;
import com.cybermanager.application.services.shared.BusinessException;
import com.cybermanager.application.usecases.session.GetCurrentSessionsUseCase;
import com.cybermanager.application.usecases.session.PaySessionUseCase;
import com.cybermanager.application.usecases.session.PauseSessionUseCase;
import com.cybermanager.application.usecases.session.ResumeSessionUseCase;
import com.cybermanager.application.usecases.session.RestartSessionsDayUseCase;
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
import com.cybermanager.domain.model.customer.DebtStatus;
import com.cybermanager.domain.model.sales.ConnectionPricingRule;
import com.cybermanager.domain.model.sales.Sale;
import com.cybermanager.domain.model.sales.SaleLine;
import com.cybermanager.domain.model.sales.SaleType;
import com.cybermanager.domain.model.session.CafeSession;
import com.cybermanager.domain.model.session.SessionId;
import com.cybermanager.domain.model.shared.Money;
import com.cybermanager.domain.port.customer.CustomerRepository;
import com.cybermanager.domain.port.customer.DebtRepository;
import com.cybermanager.domain.port.sales.ConnectionPricingRepository;
import com.cybermanager.domain.port.sales.SaleRepository;
import com.cybermanager.domain.port.session.CafeSessionRepository;
import com.cybermanager.domain.port.subscription.SubscriptionOfferRepository;
import com.cybermanager.domain.model.subscription.SubscriptionOfferId;
import com.cybermanager.domain.model.subscription.SubscriptionOfferStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        PaySessionUseCase,
        PauseSessionUseCase,
        ResumeSessionUseCase,
        RestartSessionsDayUseCase,
        SearchSessionsOfDayUseCase,
        GetCurrentSessionsUseCase {
    private static final String DEFAULT_WORKSTATION = "SESSION";
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionApplicationService.class);

    private final CafeSessionRepository sessionRepository;
    private final CustomerRepository customerRepository;
    private final ConnectionPricingRepository connectionPricingRepository;
    private final DebtRepository debtRepository;
    private final SaleRepository saleRepository;
    private final SubscriptionOfferRepository subscriptionOfferRepository;

    public SessionApplicationService(
            CafeSessionRepository sessionRepository,
            CustomerRepository customerRepository,
            ConnectionPricingRepository connectionPricingRepository,
            DebtRepository debtRepository,
            SaleRepository saleRepository,
            SubscriptionOfferRepository subscriptionOfferRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.customerRepository = customerRepository;
        this.connectionPricingRepository = connectionPricingRepository;
        this.debtRepository = debtRepository;
        this.saleRepository = saleRepository;
        this.subscriptionOfferRepository = subscriptionOfferRepository;
    }

    @Override
    public SessionView execute(StartSessionCommand command) {
        LOGGER.info(
                "Starting session customerId={} hasWalkInName={}",
                command.customerId(),
                command.customerName() != null && !command.customerName().isBlank()
        );
        Customer customer;
        if (command.customerId() != null) {
            customer = customerRepository.findById(new CustomerId(command.customerId()))
                    .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
            if (customer.type() != CustomerType.SUBSCRIBER) {
                LOGGER.warn("Session start rejected because customer is not a subscriber customerId={}", customer.id().value());
                throw new BusinessException(BusinessErrorType.VALIDATION, "SESSION_SUBSCRIBER_REQUIRED", "Only subscribers can be searched and reused");
            }
        } else if (command.customerName() != null && !command.customerName().isBlank()) {
            customer = customerRepository.save(Customer.createWalkIn(command.customerName().trim()));
            LOGGER.info("Walk-in customer created for session customerId={} name={}", customer.id().value(), customer.name());
        } else {
            LOGGER.warn("Session start rejected because no customer information was provided");
            throw new BusinessException(BusinessErrorType.VALIDATION, "SESSION_CUSTOMER_REQUIRED", "Customer name or subscriber is required");
        }

        boolean alreadyPresentInCurrentSessions = sessionRepository.findAll().stream()
                .anyMatch(session -> session.customerId().equals(customer.id()) && (session.endedAt() == null || !session.paid()));
        if (alreadyPresentInCurrentSessions) {
            LOGGER.warn("Session start rejected because customer already has a current session customerId={}", customer.id().value());
            throw new BusinessException(BusinessErrorType.CONFLICT, "SESSION_ALREADY_EXISTS_FOR_CUSTOMER", "Customer already has a current session");
        }

        if (customer.status() != CustomerStatus.ACTIVE) {
            LOGGER.warn("Session start rejected because customer is inactive customerId={}", customer.id().value());
            throw new BusinessException(BusinessErrorType.FORBIDDEN, "CUSTOMER_INACTIVE", "Customer is not active");
        }
        var session = sessionRepository.save(CafeSession.start(customer.id(), DEFAULT_WORKSTATION, LocalDateTime.now()));
        LOGGER.info("Session started sessionId={} customerId={} customerType={}", session.id().value(), customer.id().value(), customer.type());
        return toView(session, customer);
    }

    @Override
    public SessionView execute(StopSessionCommand command) {
        LOGGER.info("Stopping session sessionId={}", command.sessionId());
        var session = sessionRepository.findById(new SessionId(command.sessionId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found"));
        if (session.endedAt() != null) {
            LOGGER.warn("Stop rejected because session is already stopped sessionId={}", command.sessionId());
            throw new BusinessException(BusinessErrorType.CONFLICT, "SESSION_ALREADY_STOPPED", "Session is already stopped");
        }
        var customer = customerRepository.findById(session.customerId())
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));

        LocalDateTime stoppedAt = LocalDateTime.now();
        Money price = Money.of("0");
        if (customer.type() == CustomerType.WALK_IN) {
            ConnectionPricingRule pricing = connectionPricingRepository.getCurrentRule();
            int minutes = session.consumedMinutesUntil(stoppedAt);
            price = pricing.priceForMinutes(minutes);
            LOGGER.debug("Walk-in session pricing computed sessionId={} customerId={} consumedMinutes={} price={}", session.id().value(), customer.id().value(), minutes, price.amount());
        } else {
            int consumedMinutes = session.consumedMinutesUntil(stoppedAt);
            int availableMinutes = customer.remainingMinutes();
            int overtimeMinutes = Math.max(0, consumedMinutes - availableMinutes);
            if (overtimeMinutes > 0) {
                ConnectionPricingRule pricing = connectionPricingRepository.getCurrentRule();
                price = pricing.priceForMinutes(overtimeMinutes);
                LOGGER.debug(
                        "Subscriber overtime pricing computed sessionId={} customerId={} availableMinutes={} consumedMinutes={} overtimeMinutes={} price={}",
                        session.id().value(),
                        customer.id().value(),
                        availableMinutes,
                        consumedMinutes,
                        overtimeMinutes,
                        price.amount()
                );
            }
            customer = customerRepository.save(customer.deductMinutes(consumedMinutes));
            LOGGER.debug("Subscriber minutes deducted customerId={} consumedMinutes={} remainingMinutes={}", customer.id().value(), consumedMinutes, customer.remainingMinutes());
        }

        var stoppedSession = sessionRepository.save(session.stop(stoppedAt, price, false));
        LOGGER.info("Session stopped sessionId={} customerId={} paid=false calculatedPrice={}", stoppedSession.id().value(), customer.id().value(), price.amount());
        return toView(stoppedSession, customer);
    }

    @Override
    public SessionView execute(PaySessionCommand command) {
        LOGGER.info("Paying stopped session sessionId={} amountPaid={}", command.sessionId(), command.amountPaid());
        var session = sessionRepository.findById(new SessionId(command.sessionId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found"));
        if (session.endedAt() == null) {
            LOGGER.warn("Pay rejected because session is still active sessionId={}", command.sessionId());
            throw new BusinessException(BusinessErrorType.CONFLICT, "SESSION_NOT_STOPPED", "Session must be stopped before payment");
        }
        if (session.paid()) {
            LOGGER.warn("Pay rejected because session is already paid sessionId={}", command.sessionId());
            throw new BusinessException(BusinessErrorType.CONFLICT, "SESSION_ALREADY_PAID", "Session is already paid");
        }
        var customer = customerRepository.findById(session.customerId())
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));

        var paymentSubscriptions = applyPaymentSubscriptionOffers(session, customer, command.subscriptionOfferIds(), command.createSubscriptionDebt());
        customer = paymentSubscriptions.customer();

        BigDecimal paidAmount = command.amountPaid() == null ? BigDecimal.ZERO : command.amountPaid().max(BigDecimal.ZERO);
        BigDecimal expectedAmount = computeFinancialSnapshot(session, customer).totalAmountDue().amount();
        BigDecimal remainingAmount = expectedAmount.subtract(paidAmount).max(BigDecimal.ZERO);

        if (remainingAmount.signum() > 0) {
            var debt = DebtRecord.create(
                    customer.id(),
                    sessionDebtLabel(customer.type(), session.startedAt()),
                    new Money(remainingAmount),
                    LocalDateTime.now()
            );
            debtRepository.save(debt);
            LOGGER.info("Remaining session amount moved to debt sessionId={} customerId={} remainingAmount={}", session.id().value(), customer.id().value(), remainingAmount);
        }

        var paidSession = sessionRepository.save(session.stop(session.endedAt(), computeFinancialSnapshot(session, customer).connectionAmount(), true));
        if (customer.type() == CustomerType.SUBSCRIBER) {
            customer = customer.withRemainingMinutes(computeDisplayRemainingMinutes(session, customer, paymentSubscriptions.addedIncludedMinutes()));
            customerRepository.save(customer);
        }
        LOGGER.info("Session marked as paid sessionId={} customerId={} expectedAmount={} paidAmount={}", paidSession.id().value(), customer.id().value(), expectedAmount, paidAmount);
        return toView(paidSession, customer);
    }

    @Override
    public SessionView execute(PauseSessionCommand command) {
        LOGGER.info("Pausing session sessionId={}", command.sessionId());
        var session = sessionRepository.findById(new SessionId(command.sessionId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found"));
        if (session.paused()) {
            LOGGER.warn("Pause rejected because session is already paused sessionId={}", command.sessionId());
            throw new BusinessException(BusinessErrorType.CONFLICT, "SESSION_ALREADY_PAUSED", "Session is already paused");
        }
        var customer = customerRepository.findById(session.customerId())
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
        var pausedSession = sessionRepository.save(session.pause(LocalDateTime.now()));
        LOGGER.info("Session paused sessionId={} customerId={}", pausedSession.id().value(), customer.id().value());
        return toView(pausedSession, customer);
    }

    @Override
    public SessionView execute(ResumeSessionCommand command) {
        LOGGER.info("Resuming session sessionId={}", command.sessionId());
        var session = sessionRepository.findById(new SessionId(command.sessionId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found"));
        if (!session.paused()) {
            LOGGER.warn("Resume rejected because session is not paused sessionId={}", command.sessionId());
            throw new BusinessException(BusinessErrorType.CONFLICT, "SESSION_NOT_PAUSED", "Session is not paused");
        }
        var customer = customerRepository.findById(session.customerId())
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
        var resumedSession = sessionRepository.save(session.resume(LocalDateTime.now()));
        LOGGER.info("Session resumed sessionId={} customerId={}", resumedSession.id().value(), customer.id().value());
        return toView(resumedSession, customer);
    }

    @Transactional(readOnly = true)
    @Override
    public List<SessionView> execute(SearchSessionsOfDayQuery query) {
        LocalDate date = query.date() == null ? LocalDate.now() : query.date();
        var sessions = sessionRepository.findByDay(date).stream()
                .map(this::toView)
                .toList();
        LOGGER.debug("Sessions of day loaded date={} count={}", date, sessions.size());
        return sessions;
    }

    @Transactional(readOnly = true)
    @Override
    public CurrentSessionsView execute() {
        var sessions = sessionRepository.findAll().stream()
                .filter(session -> session.endedAt() == null || !session.paid())
                .map(this::toView)
                .toList();
        LOGGER.debug("Current sessions loaded count={}", sessions.size());
        return new CurrentSessionsView(sessions);
    }

    @Override
    public int execute(RestartSessionsDayCommand command) {
        var stoppedUnpaidSessions = sessionRepository.findAll().stream()
                .filter(session -> session.endedAt() != null && !session.paid())
                .toList();

        for (var session : stoppedUnpaidSessions) {
            var customer = customerRepository.findById(session.customerId())
                    .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
            if (session.calculatedPrice().amount().signum() > 0) {
                debtRepository.save(DebtRecord.create(
                        customer.id(),
                        sessionDebtLabel(customer.type(), session.startedAt()),
                        session.calculatedPrice(),
                        LocalDateTime.now()
                ));
            }
            createDebtsForUnpaidSales(customer.id(), session.endedAt() == null ? session.startedAt().toLocalDate() : session.endedAt().toLocalDate());
            sessionRepository.save(session.markPaid());
        }
        LOGGER.info("Sessions day restart executed archivedSessionCount={}", stoppedUnpaidSessions.size());
        return stoppedUnpaidSessions.size();
    }

    private String sessionDebtLabel(CustomerType customerType, LocalDateTime startedAt) {
        return customerType == CustomerType.SUBSCRIBER
                ? "Depassement abonnement du " + DateTimeLabelFormatter.format(startedAt)
                : "Session du " + DateTimeLabelFormatter.format(startedAt);
    }

    private PaymentSubscriptionApplicationResult applyPaymentSubscriptionOffers(CafeSession session, Customer customer, List<java.util.UUID> subscriptionOfferIds, boolean createSubscriptionDebt) {
        if (subscriptionOfferIds == null || subscriptionOfferIds.isEmpty()) {
            return new PaymentSubscriptionApplicationResult(customer, 0);
        }
        if (subscriptionOfferRepository == null) {
            throw new BusinessException(BusinessErrorType.VALIDATION, "SUBSCRIPTION_OFFERS_UNAVAILABLE", "Subscription offers are unavailable");
        }

        Customer updatedCustomer = customer;
        int addedIncludedMinutes = 0;
        for (var subscriptionOfferId : subscriptionOfferIds) {
            var offer = subscriptionOfferRepository.findById(new SubscriptionOfferId(subscriptionOfferId))
                    .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SUBSCRIPTION_OFFER_NOT_FOUND", "Subscription offer not found"));
            updatedCustomer = customerRepository.save(updatedCustomer.addSubscriptionMinutes(offer.includedMinutes()));
            addedIncludedMinutes += offer.includedMinutes();
            var offerSale = saleRepository.save(Sale.create(
                    updatedCustomer.id(),
                    session.id().value(),
                    SaleType.SUBSCRIPTION,
                    LocalDateTime.now(),
                    List.of(new SaleLine(offer.name(), 1, offer.price(), offer.price())),
                    offer.price()
            ));
            if (createSubscriptionDebt) {
                debtRepository.save(DebtRecord.create(
                        updatedCustomer.id(),
                        debtLabelForSale(offerSale),
                        offerSale.totalAmount(),
                        LocalDateTime.now()
                ));
            }
        }
        return new PaymentSubscriptionApplicationResult(updatedCustomer, addedIncludedMinutes);
    }

    private void createDebtsForUnpaidSales(CustomerId customerId, LocalDate day) {
        var existingOpenDebts = debtRepository.findByCustomerId(customerId).stream()
                .filter(debt -> debt.status() == DebtStatus.OPEN)
                .toList();
        saleRepository.findByCustomerId(customerId).stream()
                .filter(sale -> sale.soldAt().toLocalDate().equals(day))
                .filter(sale -> existingOpenDebts.stream().noneMatch(debt ->
                        debt.label().equals(debtLabelForSale(sale))
                                && debt.amount().amount().compareTo(sale.totalAmount().amount()) == 0))
                .forEach(sale -> debtRepository.save(DebtRecord.create(
                        sale.customerId(),
                        debtLabelForSale(sale),
                        sale.totalAmount(),
                        LocalDateTime.now()
                )));
    }

    private String debtLabelForSale(Sale sale) {
        return switch (sale.type()) {
            case PRODUCTS -> sale.lines().stream()
                    .map(line -> line.quantity() + " x " + line.label())
                    .reduce((left, right) -> left + ", " + right)
                    .map(label -> label + " du " + DateTimeLabelFormatter.format(sale.soldAt()))
                    .orElse("Vente produits du " + DateTimeLabelFormatter.format(sale.soldAt()));
            case SUBSCRIPTION -> "Vente abonnement du " + DateTimeLabelFormatter.format(sale.soldAt());
            case CONNECTION_TIME -> "Vente temps du " + DateTimeLabelFormatter.format(sale.soldAt());
        };
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
        var snapshot = computeFinancialSnapshot(session, customer);
        LOGGER.debug(
                "Session financial snapshot sessionId={} customerId={} customerType={} consumedMinutes={} purchases={} payablePurchases={} openDebt={} totalDue={} totalPaid={}",
                session.id().value(),
                customer.id().value(),
                customer.type(),
                consumedMinutes,
                snapshot.purchasesAmount().amount(),
                snapshot.payablePurchasesAmount(),
                snapshot.openDebtAmount().amount(),
                snapshot.totalAmountDue().amount(),
                snapshot.totalPaidAmount().amount()
        );
        return new SessionView(
                session.id().value(),
                customer.id().value(),
                customer.name(),
                customer.type().name(),
                customer.remainingMinutes(),
                computeDisplayRemainingMinutes(session, customer),
                session.workstation(),
                session.startedAt(),
                session.endedAt(),
                session.paused(),
                session.paid(),
                consumedSeconds,
                consumedMinutes,
                snapshot.connectionAmount().amount(),
                snapshot.purchasesAmount().amount(),
                snapshot.openDebtAmount().amount(),
                snapshot.totalAmountDue().amount(),
                snapshot.totalPaidAmount().amount()
        );
    }

    private int computeDisplayRemainingMinutes(CafeSession session, Customer customer) {
        return computeDisplayRemainingMinutes(session, customer, 0);
    }

    private int computeDisplayRemainingMinutes(CafeSession session, Customer customer, int extraCompensatedMinutes) {
        if (session.paid()) {
            return customer.remainingMinutes();
        }
        if (customer.type() != CustomerType.SUBSCRIBER || session.endedAt() == null || session.paid()) {
            return customer.remainingMinutes();
        }
        int compensatedMinutes = saleRepository.findByCustomerId(customer.id()).stream()
                .filter(sale -> sale.type() == SaleType.SUBSCRIPTION)
                .filter(sale -> session.id().value().equals(sale.sessionId()))
                .filter(sale -> !sale.soldAt().isBefore(session.endedAt()))
                .mapToInt(this::subscriptionIncludedMinutes)
                .sum() + Math.max(0, extraCompensatedMinutes);
        int retroConsumedMinutes = Math.min(compensatedMinutes, Math.max(0, (session.consumedSeconds() + 59) / 60));
        return Math.max(0, customer.remainingMinutes() - retroConsumedMinutes);
    }

    private int subscriptionIncludedMinutes(Sale sale) {
        return sale.lines().stream()
                .mapToInt(line -> findIncludedMinutes(line.label()))
                .sum();
    }

    private int findIncludedMinutes(String offerLabel) {
        var activeMatch = subscriptionOfferRepository == null ? List.<com.cybermanager.domain.model.subscription.SubscriptionOffer>of()
                : subscriptionOfferRepository.search(offerLabel, SubscriptionOfferStatus.ACTIVE);
        var activeExact = activeMatch.stream()
                .filter(offer -> offer.name().equalsIgnoreCase(offerLabel))
                .findFirst();
        if (activeExact.isPresent()) {
            return activeExact.get().includedMinutes();
        }
        if (subscriptionOfferRepository == null) {
            return 0;
        }
        return subscriptionOfferRepository.search(offerLabel, SubscriptionOfferStatus.INACTIVE).stream()
                .filter(offer -> offer.name().equalsIgnoreCase(offerLabel))
                .findFirst()
                .map(offer -> offer.includedMinutes())
                .orElse(0);
    }

    private SessionFinancialSnapshot computeFinancialSnapshot(CafeSession session, Customer customer) {
        LocalDateTime referenceTime = session.endedAt() == null ? LocalDateTime.now() : session.endedAt();
        int consumedMinutes = session.endedAt() == null ? session.consumedMinutesUntil(referenceTime) : Math.max(0, (session.consumedSeconds() + 59) / 60);
        Money connectionAmount = session.calculatedPrice();
        if (customer.type() == CustomerType.WALK_IN) {
            if (session.endedAt() == null) {
                connectionAmount = connectionPricingRepository.getCurrentRule().priceForMinutes(consumedMinutes);
            }
        } else {
            if (session.paid()) {
                connectionAmount = session.calculatedPrice();
            } else {
                int overtimeMinutes = Math.max(0, consumedMinutes - customer.remainingMinutes());
                Money recomputedAmount = overtimeMinutes > 0
                        ? connectionPricingRepository.getCurrentRule().priceForMinutes(overtimeMinutes)
                        : Money.of("0");
                connectionAmount = session.endedAt() == null
                        ? recomputedAmount
                        : new Money(session.calculatedPrice().amount().min(recomputedAmount.amount()));
            }
        }

        var salesOfSession = saleRepository.findByCustomerId(customer.id()).stream()
                .filter(sale -> session.id().value().equals(sale.sessionId())
                        || (sale.sessionId() == null && !sale.soldAt().isBefore(session.startedAt()) && !sale.soldAt().isAfter(referenceTime)))
                .toList();
        Money purchasesAmount = salesOfSession.stream()
                .map(Sale::totalAmount)
                .reduce(Money.of("0"), Money::add);
        var allOpenDebts = debtRepository.findByCustomerId(customer.id()).stream()
                .filter(debt -> debt.status() == DebtStatus.OPEN)
                .toList();
        Money openDebtAmount = allOpenDebts.stream()
                .map(DebtRecord::amount)
                .reduce(Money.of("0"), Money::add);
        BigDecimal payablePurchasesAmount = salesOfSession.stream()
                .filter(sale -> allOpenDebts.stream().noneMatch(debt ->
                        debt.label().equals(debtLabelForSale(sale))
                                && debt.amount().amount().compareTo(sale.totalAmount().amount()) == 0))
                .map(sale -> sale.totalAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal dueConnectionAmount = session.paid() ? BigDecimal.ZERO : connectionAmount.amount();
        BigDecimal paidConnectionAmount = session.paid() ? connectionAmount.amount() : BigDecimal.ZERO;
        return new SessionFinancialSnapshot(
                connectionAmount,
                purchasesAmount,
                openDebtAmount,
                payablePurchasesAmount,
                new Money(dueConnectionAmount.add(payablePurchasesAmount)),
                new Money(payablePurchasesAmount.add(paidConnectionAmount))
        );
    }

    private record SessionFinancialSnapshot(
            Money connectionAmount,
            Money purchasesAmount,
            Money openDebtAmount,
            BigDecimal payablePurchasesAmount,
            Money totalAmountDue,
            Money totalPaidAmount
    ) {
    }

    private record PaymentSubscriptionApplicationResult(
            Customer customer,
            int addedIncludedMinutes
    ) {
    }
}
