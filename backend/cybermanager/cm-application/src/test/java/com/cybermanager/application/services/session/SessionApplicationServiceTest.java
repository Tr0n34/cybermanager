package com.cybermanager.application.services.session;

import com.cybermanager.application.commands.session.StartSessionCommand;
import com.cybermanager.application.commands.session.PauseSessionCommand;
import com.cybermanager.application.commands.session.ResumeSessionCommand;
import com.cybermanager.application.commands.session.StopSessionCommand;
import com.cybermanager.application.services.shared.BusinessException;
import com.cybermanager.domain.model.customer.Customer;
import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.customer.CustomerStatus;
import com.cybermanager.domain.model.customer.CustomerType;
import com.cybermanager.domain.model.sales.ConnectionPricingRule;
import com.cybermanager.domain.model.sales.ConnectionPricingTier;
import com.cybermanager.domain.model.session.CafeSession;
import com.cybermanager.domain.model.session.SessionId;
import com.cybermanager.domain.model.shared.Money;
import com.cybermanager.domain.port.customer.CustomerRepository;
import com.cybermanager.domain.port.customer.DebtRepository;
import com.cybermanager.domain.port.sales.ConnectionPricingRepository;
import com.cybermanager.domain.port.sales.SaleRepository;
import com.cybermanager.domain.port.session.CafeSessionRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionApplicationServiceTest {
    @Test
    void shouldStartSessionForWalkInAndCreateCustomer() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository);

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
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository);

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
    void shouldRejectSubscriberWithoutCredit() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository);

        UUID customerUuid = UUID.randomUUID();
        Customer customer = new Customer(new CustomerId(customerUuid), "Client Abonne", CustomerType.SUBSCRIBER, CustomerStatus.ACTIVE, 0);

        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(Optional.of(customer));

        assertThrows(BusinessException.class, () -> service.execute(new StartSessionCommand(customerUuid, null)));
    }

    @Test
    void shouldDeductMinutesWhenStoppingSubscriberSession() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository);

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

        var result = service.execute(new StopSessionCommand(sessionId.value(), false));

        assertEquals("Client Abonne", result.customerName());
        assertEquals("SUBSCRIBER", result.customerType());
        assertEquals(75, result.remainingMinutes());
    }

    @Test
    void shouldApplyConfiguredPricingWhenStoppingWalkInSession() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository);

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

        var result = service.execute(new StopSessionCommand(sessionId.value(), false));

        assertEquals(new java.math.BigDecimal("4.00"), result.calculatedPrice());
        assertEquals(new java.math.BigDecimal("4.00"), result.totalAmountDue());
        assertEquals("Alice", result.customerName());
    }

    @Test
    void shouldPauseAndResumeSessionWithoutStoppingIt() {
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        SessionApplicationService service = new SessionApplicationService(sessionRepository, customerRepository, pricingRepository, debtRepository, saleRepository);

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
