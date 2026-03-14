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
import com.cybermanager.application.usecases.sales.DeleteSubscriptionSaleUseCase;
import com.cybermanager.application.usecases.sales.GetSaleDetailsUseCase;
import com.cybermanager.application.usecases.sales.SearchSalesOfDayUseCase;
import com.cybermanager.application.views.sales.ConnectionPricingView;
import com.cybermanager.application.views.sales.SaleLineView;
import com.cybermanager.application.views.sales.SaleView;
import com.cybermanager.domain.model.catalog.ProductId;
import com.cybermanager.domain.model.customer.Customer;
import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.customer.DebtRecord;
import com.cybermanager.domain.model.customer.DebtStatus;
import com.cybermanager.domain.model.sales.ConnectionPricingTier;
import com.cybermanager.domain.model.shared.Money;
import com.cybermanager.domain.model.subscription.SubscriptionOfferId;
import com.cybermanager.domain.model.subscription.SubscriptionOfferStatus;
import com.cybermanager.domain.port.catalog.ProductRepository;
import com.cybermanager.domain.port.customer.CustomerRepository;
import com.cybermanager.domain.port.customer.DebtRepository;
import com.cybermanager.domain.port.session.CafeSessionRepository;
import com.cybermanager.domain.port.subscription.SubscriptionOfferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SalesApplicationService implements
        CreateProductSaleUseCase,
        CreateSubscriptionSaleUseCase,
        DeleteSubscriptionSaleUseCase,
        CreateConnectionTimeSaleUseCase,
        ConfigureConnectionPricingUseCase,
        SearchSalesOfDayUseCase,
        GetSaleDetailsUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(SalesApplicationService.class);

    private final SaleRepository saleRepository;
    private final ConnectionPricingRepository pricingRepository;
    private final ProductRepository productRepository;
    private final SubscriptionOfferRepository subscriptionOfferRepository;
    private final CustomerRepository customerRepository;
    private final DebtRepository debtRepository;
    private final CafeSessionRepository sessionRepository;

    public SalesApplicationService(SaleRepository saleRepository, ConnectionPricingRepository pricingRepository, ProductRepository productRepository, SubscriptionOfferRepository subscriptionOfferRepository, CustomerRepository customerRepository, DebtRepository debtRepository, CafeSessionRepository sessionRepository) {
        this.saleRepository = saleRepository;
        this.pricingRepository = pricingRepository;
        this.productRepository = productRepository;
        this.subscriptionOfferRepository = subscriptionOfferRepository;
        this.customerRepository = customerRepository;
        this.debtRepository = debtRepository;
        this.sessionRepository = sessionRepository;
    }

    public SaleView execute(CreateProductSaleCommand command) {
        LOGGER.info("Creating product sale customerId={} lineCount={} createDebt={}", command.customerId(), command.lines().size(), command.createDebt());
        var lines = command.lines().stream().map(line -> {
            var product = productRepository.findById(new ProductId(line.productId())).orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product not found"));
            var total = new Money(product.price().amount().multiply(BigDecimal.valueOf(line.quantity())));
            return new SaleLine(product.name(), line.quantity(), product.price(), total);
        }).toList();
        var totalAmount = lines.stream().map(SaleLine::totalPrice).reduce(Money.of("0"), Money::add);
        validateSessionLink(command.customerId(), command.sessionId());
        var sale = saleRepository.save(Sale.create(new CustomerId(command.customerId()), command.sessionId(), SaleType.PRODUCTS, LocalDateTime.now(), lines, totalAmount));
        createDebtIfRequested(command.customerId(), command.createDebt(), debtLabelForSale(sale), totalAmount);
        LOGGER.info("Product sale created saleId={} customerId={} total={}", sale.id().value(), sale.customerId().value(), sale.totalAmount().amount());
        return toView(sale);
    }

    public SaleView execute(CreateSubscriptionSaleCommand command) {
        LOGGER.info("Creating subscription sale customerId={} offerId={} createDebt={}", command.customerId(), command.subscriptionOfferId(), command.createDebt());
        var offer = subscriptionOfferRepository.findById(new SubscriptionOfferId(command.subscriptionOfferId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SUBSCRIPTION_OFFER_NOT_FOUND", "Subscription offer not found"));
        Customer customer = customerRepository.findById(new CustomerId(command.customerId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
        customerRepository.save(customer.addSubscriptionMinutes(offer.includedMinutes()));
        var lines = List.of(new SaleLine(offer.name(), 1, offer.price(), offer.price()));
        validateSessionLink(command.customerId(), command.sessionId());
        var sale = saleRepository.save(Sale.create(new CustomerId(command.customerId()), command.sessionId(), SaleType.SUBSCRIPTION, LocalDateTime.now(), lines, offer.price()));
        createDebtIfRequested(command.customerId(), command.createDebt(), debtLabelForSale(sale), offer.price());
        LOGGER.info("Subscription sale created saleId={} customerId={} includedMinutes={}", sale.id().value(), sale.customerId().value(), offer.includedMinutes());
        return toView(sale);
    }

    public void execute(UUID saleId) {
        var sale = saleRepository.findById(new SaleId(saleId))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SALE_NOT_FOUND", "Sale not found"));
        if (sale.type() != SaleType.SUBSCRIPTION) {
            throw new BusinessException(BusinessErrorType.VALIDATION, "SALE_NOT_SUBSCRIPTION", "Only subscription sales can be deleted");
        }
        if (sale.sessionId() == null) {
            throw new BusinessException(BusinessErrorType.VALIDATION, "SALE_SESSION_REQUIRED", "Sale is not linked to a session");
        }

        var session = sessionRepository.findById(new com.cybermanager.domain.model.session.SessionId(sale.sessionId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found"));
        if (session.endedAt() == null || session.paid()) {
            throw new BusinessException(BusinessErrorType.CONFLICT, "SALE_DELETE_FORBIDDEN", "Subscription sale can only be deleted before payment");
        }

        var customer = customerRepository.findById(sale.customerId())
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
        int includedMinutes = sale.lines().stream()
                .mapToInt(line -> findIncludedMinutes(line.label()))
                .sum();
        customerRepository.save(customer.removeSubscriptionMinutes(includedMinutes));

        debtRepository.findByCustomerId(customer.id()).stream()
                .filter(debt -> debt.status() == DebtStatus.OPEN)
                .filter(debt -> debt.label().equals(debtLabelForSale(sale)))
                .filter(debt -> debt.amount().amount().compareTo(sale.totalAmount().amount()) == 0)
                .findFirst()
                .ifPresent(debt -> debtRepository.deleteById(debt.id()));

        saleRepository.deleteById(sale.id());
        LOGGER.info("Subscription sale deleted saleId={} customerId={} includedMinutes={}", sale.id().value(), customer.id().value(), includedMinutes);
    }

    public SaleView execute(CreateConnectionTimeSaleCommand command) {
        LOGGER.info("Creating connection time sale customerId={} minutes={} createDebt={}", command.customerId(), command.minutes(), command.createDebt());
        var pricing = pricingRepository.getCurrentRule();
        Money total = pricing.priceForMinutes(command.minutes());
        var lines = List.of(new SaleLine("Connection time " + command.minutes() + " min", 1, total, total));
        validateSessionLink(command.customerId(), command.sessionId());
        var sale = saleRepository.save(Sale.create(new CustomerId(command.customerId()), command.sessionId(), SaleType.CONNECTION_TIME, LocalDateTime.now(), lines, total));
        createDebtIfRequested(command.customerId(), command.createDebt(), debtLabelForSale(sale), total);
        LOGGER.debug("Connection time pricing computed customerId={} minutes={} total={}", command.customerId(), command.minutes(), total.amount());
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
        LOGGER.info("Connection pricing updated tierCount={}", tiers.size());
        return toPricingView(rule);
    }

    @Transactional(readOnly = true)
    public ConnectionPricingView getCurrentPricing() {
        return toPricingView(pricingRepository.getCurrentRule());
    }

    @Transactional(readOnly = true)
    public List<SaleView> execute(SearchSalesOfDayQuery query) {
        LocalDate date = query.date() == null ? LocalDate.now() : query.date();
        var sales = saleRepository.findByDay(date).stream().map(this::toView).toList();
        LOGGER.debug("Sales of day loaded date={} count={}", date, sales.size());
        return sales;
    }

    @Transactional(readOnly = true)
    public SaleView execute(GetSaleDetailsQuery query) {
        LOGGER.debug("Loading sale details saleId={}", query.saleId());
        return saleRepository.findById(new SaleId(query.saleId())).map(this::toView)
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SALE_NOT_FOUND", "Sale not found"));
    }

    private void createDebtIfRequested(java.util.UUID customerId, boolean createDebt, String label, Money amount) {
        if (!createDebt || amount.amount().signum() <= 0) {
            return;
        }
        debtRepository.save(DebtRecord.create(new CustomerId(customerId), label, amount, LocalDateTime.now()));
        LOGGER.info("Debt created from sale customerId={} amount={} label={}", customerId, amount.amount(), label);
    }

    private void validateSessionLink(java.util.UUID customerId, java.util.UUID sessionId) {
        if (sessionId == null) {
            return;
        }
        var session = sessionRepository.findById(new com.cybermanager.domain.model.session.SessionId(sessionId))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found"));
        if (!session.customerId().equals(new CustomerId(customerId))) {
            throw new BusinessException(BusinessErrorType.VALIDATION, "SESSION_CUSTOMER_MISMATCH", "Session does not belong to customer");
        }
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
                tier.id(),
                tier.durationMinutes() / 60,
                tier.durationMinutes() % 60,
                tier.durationMinutes(),
                tier.price().amount(),
                tier.createdAt(),
                tier.updatedAt()
        );
    }

    private int findIncludedMinutes(String offerLabel) {
        return subscriptionOfferRepository.search(offerLabel, SubscriptionOfferStatus.ACTIVE).stream()
                .filter(offer -> offer.name().equalsIgnoreCase(offerLabel))
                .findFirst()
                .or(() -> subscriptionOfferRepository.search(offerLabel, SubscriptionOfferStatus.INACTIVE).stream()
                        .filter(offer -> offer.name().equalsIgnoreCase(offerLabel))
                        .findFirst())
                .map(offer -> offer.includedMinutes())
                .orElse(0);
    }
}

