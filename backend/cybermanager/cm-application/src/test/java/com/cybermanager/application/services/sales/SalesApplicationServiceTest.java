package com.cybermanager.application.services.sales;

import com.cybermanager.application.commands.sales.ConfigureConnectionPricingCommand;
import com.cybermanager.application.commands.sales.CreateConnectionTimeSaleCommand;
import com.cybermanager.application.commands.sales.CreateSubscriptionSaleCommand;
import com.cybermanager.domain.model.customer.Customer;
import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.customer.CustomerStatus;
import com.cybermanager.domain.model.customer.CustomerType;
import com.cybermanager.domain.model.customer.DebtRecord;
import com.cybermanager.domain.model.customer.DebtStatus;
import com.cybermanager.domain.model.sales.ConnectionPricingRule;
import com.cybermanager.domain.model.sales.ConnectionPricingTier;
import com.cybermanager.domain.model.sales.Sale;
import com.cybermanager.domain.model.sales.SaleId;
import com.cybermanager.domain.model.sales.SaleLine;
import com.cybermanager.domain.model.sales.SaleType;
import com.cybermanager.domain.model.session.CafeSession;
import com.cybermanager.domain.model.session.SessionId;
import com.cybermanager.domain.model.shared.Money;
import com.cybermanager.domain.model.subscription.SubscriptionOffer;
import com.cybermanager.domain.model.subscription.SubscriptionOfferId;
import com.cybermanager.domain.model.subscription.SubscriptionOfferStatus;
import com.cybermanager.domain.port.catalog.ProductRepository;
import com.cybermanager.domain.port.customer.CustomerRepository;
import com.cybermanager.domain.port.customer.DebtRepository;
import com.cybermanager.domain.port.sales.ConnectionPricingRepository;
import com.cybermanager.domain.port.sales.SaleRepository;
import com.cybermanager.domain.port.session.CafeSessionRepository;
import com.cybermanager.domain.port.subscription.SubscriptionOfferRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SalesApplicationServiceTest {
    @Test
    void shouldCalculateConnectionSaleFromConfiguredPricingTiers() {
        SaleRepository saleRepository = mock(SaleRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        SubscriptionOfferRepository offerRepository = mock(SubscriptionOfferRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        SalesApplicationService service = new SalesApplicationService(saleRepository, pricingRepository, productRepository, offerRepository, customerRepository, debtRepository, sessionRepository);

        ConnectionPricingRule rule = new ConnectionPricingRule(List.of(
                new ConnectionPricingTier(30, Money.of("1.50")),
                new ConnectionPricingTier(60, Money.of("2.50"))
        ));

        when(pricingRepository.getCurrentRule()).thenReturn(rule);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.execute(new CreateConnectionTimeSaleCommand("admin@cybermanager.local", Set.of("ADMIN"), UUID.randomUUID(), null, 90, false));

        assertEquals(new BigDecimal("4.00"), result.totalAmount());
    }

    @Test
    void shouldExposeSortedPricingGrid() {
        SaleRepository saleRepository = mock(SaleRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        SubscriptionOfferRepository offerRepository = mock(SubscriptionOfferRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        SalesApplicationService service = new SalesApplicationService(saleRepository, pricingRepository, productRepository, offerRepository, customerRepository, debtRepository, sessionRepository);

        when(pricingRepository.save(any(ConnectionPricingRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.execute(new ConfigureConnectionPricingCommand(
                "admin@cybermanager.local",
                Set.of("ADMIN"),
                List.of(
                        new ConfigureConnectionPricingCommand.PricingTierCommand(1, 0, new BigDecimal("2.50")),
                        new ConfigureConnectionPricingCommand.PricingTierCommand(0, 30, new BigDecimal("1.50"))
                )
        ));

        assertEquals(2, result.tiers().size());
        assertEquals(30, result.tiers().get(0).durationMinutes());
        assertEquals(new BigDecimal("1.50"), result.tiers().get(0).price());
        assertEquals(60, result.tiers().get(1).durationMinutes());
    }

    @Test
    void shouldDeleteSubscriptionSaleLinkedToAwaitingPaymentSession() {
        SaleRepository saleRepository = mock(SaleRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        SubscriptionOfferRepository offerRepository = mock(SubscriptionOfferRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        SalesApplicationService service = new SalesApplicationService(saleRepository, pricingRepository, productRepository, offerRepository, customerRepository, debtRepository, sessionRepository);

        UUID customerUuid = UUID.randomUUID();
        UUID sessionUuid = UUID.randomUUID();
        UUID saleUuid = UUID.randomUUID();
        Customer customer = new Customer(new CustomerId(customerUuid), "Nadia", CustomerType.SUBSCRIBER, CustomerStatus.ACTIVE, 120);
        Sale sale = new Sale(
                new SaleId(saleUuid),
                new CustomerId(customerUuid),
                sessionUuid,
                SaleType.SUBSCRIPTION,
                java.time.LocalDateTime.now().minusMinutes(5),
                List.of(new SaleLine("Forfait 2h", 1, Money.of("7.50"), Money.of("7.50"))),
                Money.of("7.50")
        );
        CafeSession session = new CafeSession(
                new SessionId(sessionUuid),
                new CustomerId(customerUuid),
                "SESSION",
                java.time.LocalDateTime.now().minusHours(2),
                java.time.LocalDateTime.now().minusMinutes(2),
                null,
                false,
                0,
                3600,
                Money.of("4.00")
        );
        DebtRecord debt = new DebtRecord(
                com.cybermanager.domain.model.customer.DebtId.newId(),
                new CustomerId(customerUuid),
                "Vente abonnement du " + com.cybermanager.application.services.shared.DateTimeLabelFormatter.format(sale.soldAt()),
                "",
                Money.of("7.50"),
                DebtStatus.OPEN,
                java.time.LocalDateTime.now().minusMinutes(4),
                null
        );

        when(saleRepository.findById(new SaleId(saleUuid))).thenReturn(java.util.Optional.of(sale));
        when(sessionRepository.findById(new SessionId(sessionUuid))).thenReturn(java.util.Optional.of(session));
        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(java.util.Optional.of(customer));
        when(offerRepository.search("Forfait 2h", SubscriptionOfferStatus.ACTIVE)).thenReturn(List.of(
                new SubscriptionOffer(new SubscriptionOfferId(UUID.randomUUID()), "Forfait 2h", Money.of("7.50"), 120, SubscriptionOfferStatus.ACTIVE)
        ));
        when(debtRepository.findByCustomerId(new CustomerId(customerUuid))).thenReturn(List.of(debt));

        service.execute(saleUuid);

        verify(customerRepository).save(argThat(saved -> saved.remainingMinutes() == 0));
        verify(debtRepository).deleteById(debt.id());
        verify(saleRepository).deleteById(new SaleId(saleUuid));
    }

    @Test
    void shouldCreateSubscriptionSaleWithQuantity() {
        SaleRepository saleRepository = mock(SaleRepository.class);
        ConnectionPricingRepository pricingRepository = mock(ConnectionPricingRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        SubscriptionOfferRepository offerRepository = mock(SubscriptionOfferRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        SalesApplicationService service = new SalesApplicationService(saleRepository, pricingRepository, productRepository, offerRepository, customerRepository, debtRepository, sessionRepository);

        UUID customerUuid = UUID.randomUUID();
        UUID offerUuid = UUID.randomUUID();
        Customer customer = new Customer(new CustomerId(customerUuid), "Yanis", CustomerType.SUBSCRIBER, CustomerStatus.ACTIVE, 60);
        SubscriptionOffer offer = new SubscriptionOffer(new SubscriptionOfferId(offerUuid), "Forfait 5h", Money.of("12.00"), 300, SubscriptionOfferStatus.ACTIVE);

        when(offerRepository.findById(new SubscriptionOfferId(offerUuid))).thenReturn(java.util.Optional.of(offer));
        when(customerRepository.findById(new CustomerId(customerUuid))).thenReturn(java.util.Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.execute(new CreateSubscriptionSaleCommand("admin@cybermanager.local", Set.of("ADMIN"), customerUuid, null, offerUuid, 2, false));

        assertEquals(new BigDecimal("24.00"), result.totalAmount());
        assertEquals(2, result.lines().getFirst().quantity());
        verify(customerRepository).save(argThat(saved -> saved.remainingMinutes() == 660));
    }
}
