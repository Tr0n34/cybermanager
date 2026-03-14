package com.cybermanager.application.services.session;

import com.cybermanager.application.commands.session.StartSessionCommand;
import com.cybermanager.application.commands.session.PauseSessionCommand;
import com.cybermanager.application.commands.session.PaySessionCommand;
import com.cybermanager.application.commands.session.RestartSessionsDayCommand;
import com.cybermanager.application.commands.session.ResumeSessionCommand;
import com.cybermanager.application.commands.session.StopSessionCommand;
import com.cybermanager.application.services.shared.BusinessException;
import com.cybermanager.domain.model.customer.Customer;
import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.customer.CustomerStatus;
import com.cybermanager.domain.model.customer.CustomerType;
import com.cybermanager.domain.model.customer.DebtRecord;
import com.cybermanager.domain.model.sales.ConnectionPricingRule;
import com.cybermanager.domain.model.sales.ConnectionPricingTier;
import com.cybermanager.domain.model.sales.Sale;
import com.cybermanager.domain.model.sales.SaleLine;
import com.cybermanager.domain.model.sales.SaleType;
import com.cybermanager.domain.model.session.CafeSession;
import com.cybermanager.domain.model.session.SessionId;
import com.cybermanager.domain.model.shared.Money;
import com.cybermanager.domain.model.subscription.SubscriptionOffer;
import com.cybermanager.domain.model.subscription.SubscriptionOfferId;
import com.cybermanager.domain.model.subscription.SubscriptionOfferStatus;
import com.cybermanager.domain.port.customer.CustomerRepository;
import com.cybermanager.domain.port.customer.DebtRepository;
import com.cybermanager.domain.port.sales.ConnectionPricingRepository;
import com.cybermanager.domain.port.sales.SaleRepository;
import com.cybermanager.domain.port.session.CafeSessionRepository;
import com.cybermanager.domain.port.subscription.SubscriptionOfferRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionApplicationServiceTest {
    @Test
    void shouldStartSessionForWalkInAndCreateCustomer() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository, mock(SubscriptionOfferRepository.class));

        Customer created = Customer.createWalkIn("Alice");

        when(customerRepository.save(any(Customer.class))).thenReturn(created);
        when(sessionRepository.save(any(CafeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(saleRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());
        when(pricingRepository.getCurrentRule()).thenReturn(new ConnectionPricingRule(List.of(
                new ConnectionPricingTier(30, Money.of("1.50"))
        )));

        var result = service.execute(new StartSessionCommand(null, "Alice"));

        assertEquals("Alice", result.customerName());
        assertEquals("WALK_IN", result.customerType());
    }

    @Test
    void shouldStartSessionForSubscriberWithCredit() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository, mock(SubscriptionOfferRepository.class));

        UUID customerUuid = UUID.fromString("66666666-6666-6666-6666-666666666666");
        Customer customer = new Customer(new CustomerId(customerUuid), "Client Abonne", CustomerType.SUBSCRIBER, CustomerStatus.ACTIVE, 180);

        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(Optional.of(customer));
        when(sessionRepository.save(any(CafeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(saleRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());

        var result = service.execute(new StartSessionCommand(customerUuid, null));

        assertEquals(customerUuid, result.customerId());
        assertEquals("Client Abonne", result.customerName());
        assertEquals("SUBSCRIBER", result.customerType());
    }

    @Test
    void shouldStartSubscriberWithoutCreditAndChargeOvertimeLater() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository, mock(SubscriptionOfferRepository.class));

        UUID customerUuid = UUID.randomUUID();
        Customer customer = new Customer(new CustomerId(customerUuid), "Client Abonne", CustomerType.SUBSCRIBER, CustomerStatus.ACTIVE, 0);

        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(Optional.of(customer));
        when(sessionRepository.findAll()).thenReturn(List.of());
        when(sessionRepository.save(any(CafeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(saleRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());
        when(pricingRepository.getCurrentRule()).thenReturn(new ConnectionPricingRule(List.of(
                new ConnectionPricingTier(30, Money.of("2.00")),
                new ConnectionPricingTier(60, Money.of("4.00"))
        )));

        var result = service.execute(new StartSessionCommand(customerUuid, null));

        assertEquals(customerUuid, result.customerId());
        assertEquals("SUBSCRIBER", result.customerType());
        assertEquals(0, result.remainingMinutes());
    }

    @Test
    void shouldRejectSecondCurrentSessionForSameCustomer() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository, mock(SubscriptionOfferRepository.class));

        UUID customerUuid = UUID.randomUUID();
        Customer customer = new Customer(new CustomerId(customerUuid), "Client Abonne", CustomerType.SUBSCRIBER, CustomerStatus.ACTIVE, 120);
        CafeSession currentSession = new CafeSession(
                new SessionId(UUID.randomUUID()),
                customer.id(),
                "SESSION",
                LocalDateTime.now().minusMinutes(20),
                null,
                null,
                false,
                0,
                0,
                Money.of("0")
        );

        when(customerRepository.findById(customer.id())).thenReturn(Optional.of(customer));
        when(sessionRepository.findAll()).thenReturn(List.of(currentSession));

        assertThrows(BusinessException.class, () -> service.execute(new StartSessionCommand(customerUuid, null)));
    }

    @Test
    void shouldRejectNewSessionWhenSubscriberAlreadyHasStoppedUnpaidSession() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository, mock(SubscriptionOfferRepository.class));

        UUID customerUuid = UUID.randomUUID();
        Customer customer = new Customer(new CustomerId(customerUuid), "Client Abonne", CustomerType.SUBSCRIBER, CustomerStatus.ACTIVE, 120);
        CafeSession stoppedUnpaidSession = new CafeSession(
                new SessionId(UUID.randomUUID()),
                customer.id(),
                "SESSION",
                LocalDateTime.now().minusMinutes(50),
                LocalDateTime.now().minusMinutes(5),
                null,
                false,
                0,
                1200,
                Money.of("4.00")
        );

        when(customerRepository.findById(customer.id())).thenReturn(Optional.of(customer));
        when(sessionRepository.findAll()).thenReturn(List.of(stoppedUnpaidSession));

        assertThrows(BusinessException.class, () -> service.execute(new StartSessionCommand(customerUuid, null)));
    }

    @Test
    void shouldDeductMinutesWhenStoppingSubscriberSession() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository, mock(SubscriptionOfferRepository.class));

        UUID customerUuid = UUID.randomUUID();
        SessionId sessionId = new SessionId(UUID.randomUUID());
        Customer customer = new Customer(new CustomerId(customerUuid), "Client Abonne", CustomerType.SUBSCRIBER, CustomerStatus.ACTIVE, 120);
        CafeSession session = new CafeSession(
                sessionId,
                new CustomerId(customerUuid),
                "SESSION",
                LocalDateTime.now().minusMinutes(45),
                null,
                null,
                false,
                0,
                0,
                Money.of("0")
        );
        Customer updatedCustomer = customer.deductMinutes(45);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(updatedCustomer);
        when(sessionRepository.save(any(CafeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(saleRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());

        var result = service.execute(new StopSessionCommand(sessionId.value()));

        assertEquals("Client Abonne", result.customerName());
        assertEquals("SUBSCRIBER", result.customerType());
        assertEquals(75, result.remainingMinutes());
    }

    @Test
    void shouldNotCreateDebtForSubscriberOvertimeWhenStoppingSessionBeforePayment() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository, mock(SubscriptionOfferRepository.class));

        UUID customerUuid = UUID.randomUUID();
        SessionId sessionId = new SessionId(UUID.randomUUID());
        Customer customer = new Customer(new CustomerId(customerUuid), "Abonne depasse", CustomerType.SUBSCRIBER, CustomerStatus.ACTIVE, 30);
        CafeSession session = new CafeSession(
                sessionId,
                new CustomerId(customerUuid),
                "SESSION",
                LocalDateTime.now().minusMinutes(90),
                null,
                null,
                false,
                0,
                0,
                Money.of("0")
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer.deductMinutes(90));
        when(sessionRepository.save(any(CafeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pricingRepository.getCurrentRule()).thenReturn(new ConnectionPricingRule(List.of(
                new ConnectionPricingTier(30, Money.of("2.00")),
                new ConnectionPricingTier(60, Money.of("4.00"))
        )));
        when(saleRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());
        when(debtRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());

        var result = service.execute(new StopSessionCommand(sessionId.value()));

        assertEquals(new java.math.BigDecimal("4.00"), result.calculatedPrice());
        assertEquals(new java.math.BigDecimal("4.00"), result.totalAmountDue());
        verify(debtRepository, never()).save(any(DebtRecord.class));
    }

    @Test
    void shouldCreateDebtForSubscriberOvertimeWhenPartialPaymentLeavesRemainingAmount() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository, mock(SubscriptionOfferRepository.class));

        UUID customerUuid = UUID.randomUUID();
        SessionId sessionId = new SessionId(UUID.randomUUID());
        Customer customer = new Customer(new CustomerId(customerUuid), "Abonne paye", CustomerType.SUBSCRIBER, CustomerStatus.ACTIVE, 30);
        CafeSession session = new CafeSession(
                sessionId,
                new CustomerId(customerUuid),
                "SESSION",
                LocalDateTime.now().minusMinutes(90),
                null,
                null,
                false,
                0,
                0,
                Money.of("0")
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer.deductMinutes(90));
        when(sessionRepository.save(any(CafeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pricingRepository.getCurrentRule()).thenReturn(new ConnectionPricingRule(List.of(
                new ConnectionPricingTier(30, Money.of("2.00")),
                new ConnectionPricingTier(60, Money.of("4.00"))
        )));
        when(saleRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());
        when(debtRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());

        CafeSession stoppedSession = session.stop(LocalDateTime.now(), Money.of("4.00"), false);

        when(sessionRepository.save(any(CafeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(stoppedSession));
        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(Optional.of(customer));
        when(saleRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());
        when(debtRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());

        var result = service.execute(new PaySessionCommand(sessionId.value(), new java.math.BigDecimal("2.00"), List.of(), false));

        assertEquals(new java.math.BigDecimal("4.00"), result.calculatedPrice());
        assertEquals(new java.math.BigDecimal("4.00"), result.totalPaidAmount());
        verify(debtRepository).save(argThat((DebtRecord debt) ->
                debt.label().startsWith("Depassement abonnement du ") &&
                        debt.amount().amount().compareTo(new java.math.BigDecimal("2.00")) == 0
        ));
    }

    @Test
    void shouldReduceStoppedSubscriberOvertimeWhenNewSubscriptionMinutesWereAddedBeforePayment() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository, mock(SubscriptionOfferRepository.class));

        UUID customerUuid = UUID.randomUUID();
        SessionId sessionId = new SessionId(UUID.randomUUID());
        Customer customerAfterExtraSubscriptions = new Customer(new CustomerId(customerUuid), "Abonne complete", CustomerType.SUBSCRIBER, CustomerStatus.ACTIVE, 180);
        CafeSession stoppedSession = new CafeSession(
                sessionId,
                new CustomerId(customerUuid),
                "SESSION",
                LocalDateTime.now().minusMinutes(287),
                LocalDateTime.now(),
                null,
                false,
                0,
                287 * 60,
                Money.of("17.00")
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(stoppedSession));
        when(sessionRepository.save(any(CafeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(Optional.of(customerAfterExtraSubscriptions));
        when(pricingRepository.getCurrentRule()).thenReturn(new ConnectionPricingRule(List.of(
                new ConnectionPricingTier(15, Money.of("1.00")),
                new ConnectionPricingTier(30, Money.of("2.00")),
                new ConnectionPricingTier(60, Money.of("4.00")),
                new ConnectionPricingTier(120, Money.of("7.50"))
        )));
        when(saleRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());
        when(debtRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());

        var result = service.execute(new PaySessionCommand(sessionId.value(), new java.math.BigDecimal("8.00"), List.of(), false));

        assertEquals(new java.math.BigDecimal("7.50"), result.calculatedPrice());
        assertEquals(new java.math.BigDecimal("7.50"), result.totalPaidAmount());
        verify(debtRepository, never()).save(any(DebtRecord.class));
    }

    @Test
    void shouldAddSubscriptionDuringPaymentAndRecomputeSessionAmount() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SubscriptionOfferRepository subscriptionOfferRepository = mock(SubscriptionOfferRepository.class);
        SessionApplicationService service = new SessionApplicationService(
                sessionRepository,
                customerRepository,
                pricingRepository,
                debtRepository,
                saleRepository,
                subscriptionOfferRepository
        );

        UUID customerUuid = UUID.randomUUID();
        UUID offerUuid = UUID.randomUUID();
        SessionId sessionId = new SessionId(UUID.randomUUID());
        Customer customer = new Customer(new CustomerId(customerUuid), "Abonne paiement", CustomerType.SUBSCRIBER, CustomerStatus.ACTIVE, 0);
        Customer customerWithNewOffer = customer.addSubscriptionMinutes(120);
        CafeSession stoppedSession = new CafeSession(
                sessionId,
                new CustomerId(customerUuid),
                "SESSION",
                LocalDateTime.now().minusMinutes(90),
                LocalDateTime.now(),
                null,
                false,
                0,
                90 * 60,
                Money.of("4.00")
        );
        SubscriptionOffer offer = new SubscriptionOffer(
                new SubscriptionOfferId(offerUuid),
                "Forfait 2h",
                Money.of("7.50"),
                120,
                SubscriptionOfferStatus.ACTIVE
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(stoppedSession));
        when(sessionRepository.save(any(CafeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customerWithNewOffer);
        when(subscriptionOfferRepository.findById(new SubscriptionOfferId(offerUuid))).thenReturn(Optional.of(offer));
        when(pricingRepository.getCurrentRule()).thenReturn(new ConnectionPricingRule(List.of(
                new ConnectionPricingTier(30, Money.of("2.00")),
                new ConnectionPricingTier(60, Money.of("4.00"))
        )));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(saleRepository.findByCustomerId(new CustomerId(customerUuid))).thenReturn(List.of(
                Sale.create(customer.id(), sessionId.value(), SaleType.SUBSCRIPTION, LocalDateTime.now(), List.of(new SaleLine("Forfait 2h", 1, Money.of("7.50"), Money.of("7.50"))), Money.of("7.50"))
        ));
        when(debtRepository.findByCustomerId(new CustomerId(customerUuid))).thenReturn(List.of());

        var result = service.execute(new PaySessionCommand(
                sessionId.value(),
                new java.math.BigDecimal("7.50"),
                List.of(offerUuid),
                false
        ));

        assertEquals(new java.math.BigDecimal("0.00"), result.calculatedPrice());
        assertEquals(new java.math.BigDecimal("7.50"), result.totalPaidAmount());
        assertEquals(30, result.remainingMinutes());
        verify(debtRepository, never()).save(any(DebtRecord.class));
        verify(customerRepository).save(argThat(saved -> saved.remainingMinutes() == 30));
    }

    @Test
    void shouldExposeSubscriberOvertimeAmountOnActiveSessionView() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository, mock(SubscriptionOfferRepository.class));

        UUID customerUuid = UUID.randomUUID();
        Customer customer = new Customer(new CustomerId(customerUuid), "Abonne live", CustomerType.SUBSCRIBER, CustomerStatus.ACTIVE, 30);
        CafeSession session = new CafeSession(
                new SessionId(UUID.randomUUID()),
                new CustomerId(customerUuid),
                "SESSION",
                LocalDateTime.now().minusMinutes(90),
                null,
                null,
                false,
                0,
                0,
                Money.of("0")
        );

        when(sessionRepository.findAll()).thenReturn(List.of(session));
        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(Optional.of(customer));
        when(pricingRepository.getCurrentRule()).thenReturn(new ConnectionPricingRule(List.of(
                new ConnectionPricingTier(30, Money.of("2.00")),
                new ConnectionPricingTier(60, Money.of("4.00"))
        )));
        when(saleRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());
        when(debtRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());

        var result = service.execute();

        assertEquals(1, result.sessions().size());
        assertEquals(new java.math.BigDecimal("4.00"), result.sessions().getFirst().totalAmountDue());
    }

    @Test
    void shouldApplyConfiguredPricingWhenStoppingWalkInSession() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository, mock(SubscriptionOfferRepository.class));

        UUID customerUuid = UUID.randomUUID();
        SessionId sessionId = new SessionId(UUID.randomUUID());
        Customer customer = new Customer(new CustomerId(customerUuid), "Alice", CustomerType.WALK_IN, CustomerStatus.ACTIVE, 0);
        CafeSession session = new CafeSession(
                sessionId,
                new CustomerId(customerUuid),
                "SESSION",
                LocalDateTime.now().minusMinutes(90),
                null,
                null,
                false,
                0,
                0,
                Money.of("0")
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(Optional.of(customer));
        when(sessionRepository.save(any(CafeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pricingRepository.getCurrentRule()).thenReturn(new ConnectionPricingRule(List.of(
                new ConnectionPricingTier(30, Money.of("1.50")),
                new ConnectionPricingTier(60, Money.of("2.50"))
        )));
        when(saleRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());

        var result = service.execute(new StopSessionCommand(sessionId.value()));

        assertEquals(new java.math.BigDecimal("4.00"), result.calculatedPrice());
        assertEquals(new java.math.BigDecimal("4.00"), result.totalAmountDue());
        assertEquals("Alice", result.customerName());
    }

    @Test
    void shouldKeepStoppedUnpaidSessionInCurrentSessions() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository, mock(SubscriptionOfferRepository.class));

        UUID customerUuid = UUID.randomUUID();
        Customer customer = new Customer(new CustomerId(customerUuid), "Client attente", CustomerType.WALK_IN, CustomerStatus.ACTIVE, 0);
        CafeSession pendingPaymentSession = new CafeSession(
                new SessionId(UUID.randomUUID()),
                new CustomerId(customerUuid),
                "SESSION",
                LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().minusMinutes(2),
                null,
                false,
                0,
                1800,
                Money.of("4.00")
        );

        when(sessionRepository.findAll()).thenReturn(List.of(pendingPaymentSession));
        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(Optional.of(customer));
        when(saleRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());
        when(debtRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());

        var result = service.execute();

        assertEquals(1, result.sessions().size());
        assertEquals(new java.math.BigDecimal("4.00"), result.sessions().getFirst().totalAmountDue());
    }

    @Test
    void shouldArchiveStoppedUnpaidSessionsWhenRestartingDay() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository, mock(SubscriptionOfferRepository.class));

        UUID customerUuid = UUID.randomUUID();
        Customer customer = new Customer(new CustomerId(customerUuid), "Client attente", CustomerType.WALK_IN, CustomerStatus.ACTIVE, 0);
        CafeSession pendingPaymentSession = new CafeSession(
                new SessionId(UUID.randomUUID()),
                new CustomerId(customerUuid),
                "SESSION",
                LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().minusMinutes(2),
                null,
                false,
                0,
                1800,
                Money.of("4.00")
        );

        when(sessionRepository.findAll()).thenReturn(List.of(pendingPaymentSession));
        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(Optional.of(customer));
        when(sessionRepository.save(any(CafeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.execute(new RestartSessionsDayCommand());

        assertEquals(1, result);
        verify(debtRepository).save(argThat((DebtRecord debt) ->
                debt.label().startsWith("Session du ") &&
                        debt.amount().amount().compareTo(new java.math.BigDecimal("4.00")) == 0
        ));
    }

    @Test
    void shouldMoveUnpaidSalesToDebtWhenRestartingDay() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository, mock(SubscriptionOfferRepository.class));

        UUID customerUuid = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Customer customer = new Customer(new CustomerId(customerUuid), "Client attente", CustomerType.WALK_IN, CustomerStatus.ACTIVE, 0);
        CafeSession pendingPaymentSession = new CafeSession(
                new SessionId(UUID.randomUUID()),
                new CustomerId(customerUuid),
                "SESSION",
                now.minusMinutes(45),
                now.minusMinutes(2),
                null,
                false,
                0,
                1800,
                Money.of("4.00")
        );
        Sale unpaidProductSale = Sale.create(
                customer.id(),
                SaleType.PRODUCTS,
                now.minusMinutes(20),
                List.of(new SaleLine("Snack", 2, Money.of("1.50"), Money.of("3.00"))),
                Money.of("3.00")
        );

        when(sessionRepository.findAll()).thenReturn(List.of(pendingPaymentSession));
        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(Optional.of(customer));
        when(sessionRepository.save(any(CafeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(saleRepository.findByCustomerId(customer.id())).thenReturn(List.of(unpaidProductSale));
        when(debtRepository.findByCustomerId(customer.id())).thenReturn(List.of());

        var result = service.execute(new RestartSessionsDayCommand());

        assertEquals(1, result);
        verify(debtRepository).save(argThat((DebtRecord debt) ->
                debt.label().startsWith("2 x Snack du ")
                        && debt.amount().amount().compareTo(new java.math.BigDecimal("3.00")) == 0
        ));
    }

    @Test
    void shouldExcludeSalesAlreadyPutInDebtFromAmountDue() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository, mock(SubscriptionOfferRepository.class));

        UUID customerUuid = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Customer customer = new Customer(new CustomerId(customerUuid), "Client caisse", CustomerType.WALK_IN, CustomerStatus.ACTIVE, 0);
        CafeSession session = new CafeSession(
                new SessionId(UUID.randomUUID()),
                new CustomerId(customerUuid),
                "SESSION",
                now.minusHours(1),
                null,
                null,
                false,
                0,
                0,
                Money.of("0")
        );
        Sale paidSale = Sale.create(
                customer.id(),
                SaleType.PRODUCTS,
                now.minusMinutes(15),
                List.of(new SaleLine("Cafe", 1, Money.of("2.00"), Money.of("2.00"))),
                Money.of("2.00")
        );
        Sale debtSale = Sale.create(
                customer.id(),
                SaleType.PRODUCTS,
                now.minusMinutes(10),
                List.of(new SaleLine("Snack", 1, Money.of("1.50"), Money.of("1.50"))),
                Money.of("1.50")
        );

        when(sessionRepository.findAll()).thenReturn(List.of(session));
        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(Optional.of(customer));
        when(pricingRepository.getCurrentRule()).thenReturn(new ConnectionPricingRule(List.of(
                new ConnectionPricingTier(60, Money.of("3.00"))
        )));
        when(saleRepository.findByCustomerId(customer.id())).thenReturn(List.of(paidSale, debtSale));
        when(debtRepository.findByCustomerId(customer.id())).thenReturn(List.of(
                DebtRecord.create(customer.id(), "1 x Snack du " + com.cybermanager.application.services.shared.DateTimeLabelFormatter.format(now.minusMinutes(10)), Money.of("1.50"), now.minusMinutes(9))
        ));

        var result = service.execute();

        assertEquals(1, result.sessions().size());
        assertEquals(new java.math.BigDecimal("5.00"), result.sessions().getFirst().totalAmountDue());
    }

    @Test
    void shouldKeepNonDebtSalesInAmountDueEvenWhenAnotherOpenDebtHasSameAmount() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository, mock(SubscriptionOfferRepository.class));

        UUID customerUuid = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Customer customer = new Customer(new CustomerId(customerUuid), "Client session", CustomerType.WALK_IN, CustomerStatus.ACTIVE, 0);
        CafeSession session = new CafeSession(
                new SessionId(UUID.randomUUID()),
                new CustomerId(customerUuid),
                "SESSION",
                now.minusHours(2),
                null,
                null,
                false,
                0,
                0,
                Money.of("0")
        );
        Sale nonDebtSaleOfSession = Sale.create(
                customer.id(),
                SaleType.PRODUCTS,
                now.minusMinutes(30),
                List.of(new SaleLine("Casque", 1, Money.of("8.00"), Money.of("8.00"))),
                Money.of("8.00")
        );
        DebtRecord unrelatedOpenDebt = DebtRecord.create(
                customer.id(),
                "Ancienne dette libre",
                Money.of("8.00"),
                now.minusDays(1)
        );

        when(sessionRepository.findAll()).thenReturn(List.of(session));
        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(Optional.of(customer));
        when(pricingRepository.getCurrentRule()).thenReturn(new ConnectionPricingRule(List.of(
                new ConnectionPricingTier(60, Money.of("3.00"))
        )));
        when(saleRepository.findByCustomerId(customer.id())).thenReturn(List.of(nonDebtSaleOfSession));
        when(debtRepository.findByCustomerId(customer.id())).thenReturn(List.of(unrelatedOpenDebt));

        var result = service.execute();

        assertEquals(1, result.sessions().size());
        assertEquals(new java.math.BigDecimal("14.00"), result.sessions().getFirst().totalAmountDue());
    }

    @Test
    void shouldPauseAndResumeSessionWithoutStoppingIt() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository, mock(SubscriptionOfferRepository.class));

        UUID customerUuid = UUID.randomUUID();
        SessionId sessionId = new SessionId(UUID.randomUUID());
        Customer customer = new Customer(new CustomerId(customerUuid), "Pause Test", CustomerType.WALK_IN, CustomerStatus.ACTIVE, 0);
        CafeSession session = new CafeSession(
                sessionId,
                new CustomerId(customerUuid),
                "SESSION",
                LocalDateTime.now().minusMinutes(20),
                null,
                null,
                false,
                0,
                0,
                Money.of("0")
        );

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(Optional.of(customer));
        when(sessionRepository.save(any(CafeSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pricingRepository.getCurrentRule()).thenReturn(new ConnectionPricingRule(List.of(new ConnectionPricingTier(30, Money.of("1.50")))));
        when(saleRepository.findByCustomerId(any(CustomerId.class))).thenReturn(List.of());

        var paused = service.execute(new PauseSessionCommand(sessionId.value()));
        assertEquals(true, paused.paused());

        var pausedSession = new CafeSession(
                sessionId,
                new CustomerId(customerUuid),
                "SESSION",
                session.startedAt(),
                null,
                LocalDateTime.now().minusMinutes(5),
                false,
                0,
                0,
                Money.of("0")
        );
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(pausedSession));

        var resumed = service.execute(new ResumeSessionCommand(sessionId.value()));
        assertEquals(false, resumed.paused());
    }
}
