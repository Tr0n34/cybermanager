package com.cybermanager.application.services.sales;

import com.cybermanager.application.commands.sales.ConfigureConnectionPricingCommand;
import com.cybermanager.application.commands.sales.CreateConnectionTimeSaleCommand;
import com.cybermanager.application.commands.sales.CreateProductSaleCommand;
import com.cybermanager.application.commands.sales.CreateSubscriptionSaleCommand;
import com.cybermanager.application.queries.sales.GetSaleDetailsQuery;
import com.cybermanager.application.queries.sales.SearchSalesOfDayQuery;
import com.cybermanager.application.services.shared.DateTimeLabelFormatter;
import com.cybermanager.application.services.shared.BusinessErrorType;
import com.cybermanager.application.services.shared.BusinessException;
import com.cybermanager.domain.model.sales.ConnectionPricingRule;
import com.cybermanager.domain.model.sales.Sale;
import com.cybermanager.domain.model.sales.SaleId;
import com.cybermanager.domain.model.sales.SaleLine;
import com.cybermanager.domain.model.sales.SaleType;
import com.cybermanager.domain.port.sales.ConnectionPricingRepository;
import com.cybermanager.domain.port.sales.SaleRepository;
import com.cybermanager.application.services.shared.ActorSupport;
import com.cybermanager.application.usecases.sales.ConfigureConnectionPricingUseCase;
import com.cybermanager.application.usecases.sales.CreateConnectionTimeSaleUseCase;
import com.cybermanager.application.usecases.sales.CreateProductSaleUseCase;
import com.cybermanager.application.usecases.sales.CreateSubscriptionSaleUseCase;
import com.cybermanager.application.usecases.sales.GetSaleDetailsUseCase;
import com.cybermanager.application.usecases.sales.SearchSalesOfDayUseCase;
import com.cybermanager.application.views.sales.ConnectionPricingView;
import com.cybermanager.application.views.sales.SaleLineView;
import com.cybermanager.application.views.sales.SaleView;
import com.cybermanager.domain.model.catalog.ProductId;
import com.cybermanager.domain.model.customer.Customer;
import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.customer.DebtRecord;
import com.cybermanager.domain.model.sales.ConnectionPricingTier;
import com.cybermanager.domain.model.shared.Money;
import com.cybermanager.domain.model.subscription.SubscriptionOfferId;
import com.cybermanager.domain.port.catalog.ProductRepository;
import com.cybermanager.domain.port.customer.CustomerRepository;
import com.cybermanager.domain.port.customer.DebtRepository;
import com.cybermanager.domain.port.subscription.SubscriptionOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SalesApplicationService implements
        CreateProductSaleUseCase,
        CreateSubscriptionSaleUseCase,
        CreateConnectionTimeSaleUseCase,
        ConfigureConnectionPricingUseCase,
        SearchSalesOfDayUseCase,
        GetSaleDetailsUseCase {
    private final SaleRepository saleRepository;
    private final ConnectionPricingRepository pricingRepository;
    private final ProductRepository productRepository;
    private final SubscriptionOfferRepository subscriptionOfferRepository;
    private final CustomerRepository customerRepository;
    private final DebtRepository debtRepository;

    public SalesApplicationService(SaleRepository saleRepository, ConnectionPricingRepository pricingRepository, ProductRepository productRepository, SubscriptionOfferRepository subscriptionOfferRepository, CustomerRepository customerRepository, DebtRepository debtRepository) {
        this.saleRepository = saleRepository;
        this.pricingRepository = pricingRepository;
        this.productRepository = productRepository;
        this.subscriptionOfferRepository = subscriptionOfferRepository;
        this.customerRepository = customerRepository;
        this.debtRepository = debtRepository;
    }

    public SaleView execute(CreateProductSaleCommand command) {
        var lines = command.lines().stream().map(line -> {
            var product = productRepository.findById(new ProductId(line.productId())).orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product not found"));
            var total = new Money(product.price().amount().multiply(BigDecimal.valueOf(line.quantity())));
            return new SaleLine(product.name(), line.quantity(), product.price(), total);
        }).toList();
        var totalAmount = lines.stream().map(SaleLine::totalPrice).reduce(Money.of("0"), Money::add);
        var sale = saleRepository.save(Sale.create(new CustomerId(command.customerId()), SaleType.PRODUCTS, LocalDateTime.now(), lines, totalAmount));
        createDebtIfRequested(command.customerId(), command.createDebt(), "Vente produits du " + DateTimeLabelFormatter.format(sale.soldAt()), totalAmount);
        return toView(sale);
    }

    public SaleView execute(CreateSubscriptionSaleCommand command) {
        var offer = subscriptionOfferRepository.findById(new SubscriptionOfferId(command.subscriptionOfferId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SUBSCRIPTION_OFFER_NOT_FOUND", "Subscription offer not found"));
        Customer customer = customerRepository.findById(new CustomerId(command.customerId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
        customerRepository.save(customer.addSubscriptionMinutes(offer.includedMinutes()));
        var lines = List.of(new SaleLine(offer.name(), 1, offer.price(), offer.price()));
        var sale = saleRepository.save(Sale.create(new CustomerId(command.customerId()), SaleType.SUBSCRIPTION, LocalDateTime.now(), lines, offer.price()));
        createDebtIfRequested(command.customerId(), command.createDebt(), "Vente abonnement du " + DateTimeLabelFormatter.format(sale.soldAt()), offer.price());
        return toView(sale);
    }

    public SaleView execute(CreateConnectionTimeSaleCommand command) {
        var pricing = pricingRepository.getCurrentRule();
        Money total = pricing.priceForMinutes(command.minutes());
        var lines = List.of(new SaleLine("Connection time " + command.minutes() + " min", 1, total, total));
        var sale = saleRepository.save(Sale.create(new CustomerId(command.customerId()), SaleType.CONNECTION_TIME, LocalDateTime.now(), lines, total));
        createDebtIfRequested(command.customerId(), command.createDebt(), "Vente temps du " + DateTimeLabelFormatter.format(sale.soldAt()), total);
        return toView(sale);
    }

    public ConnectionPricingView execute(ConfigureConnectionPricingCommand command) {
        ActorSupport.requireAdmin(command.actorRoles());
        var tiers = command.tiers().stream()
                .map(tier -> new ConnectionPricingTier(
                        tier.hours() * 60 + tier.minutes(),
                        new Money(tier.price())
                ))
                .toList();
        var rule = pricingRepository.save(new ConnectionPricingRule(tiers));
        return toPricingView(rule);
    }

    @Transactional(readOnly = true)
    public ConnectionPricingView getCurrentPricing() {
        return toPricingView(pricingRepository.getCurrentRule());
    }

    @Transactional(readOnly = true)
    public List<SaleView> execute(SearchSalesOfDayQuery query) {
        LocalDate date = query.date() == null ? LocalDate.now() : query.date();
        return saleRepository.findByDay(date).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public SaleView execute(GetSaleDetailsQuery query) {
        return saleRepository.findById(new SaleId(query.saleId())).map(this::toView)
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SALE_NOT_FOUND", "Sale not found"));
    }

    private void createDebtIfRequested(java.util.UUID customerId, boolean createDebt, String label, Money amount) {
        if (!createDebt || amount.amount().signum() <= 0) {
            return;
        }
        debtRepository.save(DebtRecord.create(new CustomerId(customerId), label, amount, LocalDateTime.now()));
    }

    private SaleView toView(Sale sale) {
        return new SaleView(
                sale.id().value(),
                sale.customerId().value(),
                sale.type().name(),
                sale.soldAt(),
                sale.lines().stream().map(line -> new SaleLineView(line.label(), line.quantity(), line.unitPrice().amount(), line.totalPrice().amount())).toList(),
                sale.totalAmount().amount()
        );
    }

    private ConnectionPricingView toPricingView(ConnectionPricingRule rule) {
        return new ConnectionPricingView(rule.tiers().stream()
                .map(this::toPricingTierView)
                .toList());
    }

    private ConnectionPricingView.PricingTierView toPricingTierView(ConnectionPricingTier tier) {
        return new ConnectionPricingView.PricingTierView(
                tier.durationMinutes() / 60,
                tier.durationMinutes() % 60,
                tier.durationMinutes(),
                tier.price().amount()
        );
    }
}

