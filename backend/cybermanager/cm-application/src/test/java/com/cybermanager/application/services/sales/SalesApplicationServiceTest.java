package com.cybermanager.application.services.sales;

import com.cybermanager.application.commands.sales.ConfigureConnectionPricingCommand;
import com.cybermanager.application.commands.sales.CreateConnectionTimeSaleCommand;
import com.cybermanager.domain.model.sales.ConnectionPricingRule;
import com.cybermanager.domain.model.sales.ConnectionPricingTier;
import com.cybermanager.domain.model.sales.Sale;
import com.cybermanager.domain.model.shared.Money;
import com.cybermanager.domain.port.catalog.ProductRepository;
import com.cybermanager.domain.port.customer.CustomerRepository;
import com.cybermanager.domain.port.customer.DebtRepository;
import com.cybermanager.domain.port.sales.ConnectionPricingRepository;
import com.cybermanager.domain.port.sales.SaleRepository;
import com.cybermanager.domain.port.subscription.SubscriptionOfferRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
        SalesApplicationService service = new SalesApplicationService(saleRepository, pricingRepository, productRepository, offerRepository, customerRepository, debtRepository);

        ConnectionPricingRule rule = new ConnectionPricingRule(List.of(
                new ConnectionPricingTier(30, Money.of("1.50")),
                new ConnectionPricingTier(60, Money.of("2.50"))
        ));

        when(pricingRepository.getCurrentRule()).thenReturn(rule);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.execute(new CreateConnectionTimeSaleCommand("admin@cybermanager.local", Set.of("ADMIN"), UUID.randomUUID(), 90, false));

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
        SalesApplicationService service = new SalesApplicationService(saleRepository, pricingRepository, productRepository, offerRepository, customerRepository, debtRepository);

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
}
