package com.cybermanager.application.services.customer;

import com.cybermanager.application.commands.customer.ConvertCustomerToSubscriberCommand;
import com.cybermanager.application.commands.customer.CreateDebtFromSaleCommand;
import com.cybermanager.application.commands.customer.CreateCustomerCommand;
import com.cybermanager.application.commands.customer.ReattachDebtToSessionCommand;
import com.cybermanager.application.commands.customer.SettleDebtCommand;
import com.cybermanager.application.commands.customer.UpdateCustomerCommand;
import com.cybermanager.application.services.shared.BusinessErrorType;
import com.cybermanager.application.services.shared.BusinessException;
import com.cybermanager.application.queries.customer.GetCustomerDetailsQuery;
import com.cybermanager.application.queries.customer.SearchOpenDebtsQuery;
import com.cybermanager.application.queries.customer.SearchCustomersQuery;
import com.cybermanager.application.usecases.customer.ConvertCustomerToSubscriberUseCase;
import com.cybermanager.application.usecases.customer.CreateDebtFromSaleUseCase;
import com.cybermanager.application.usecases.customer.CreateCustomerUseCase;
import com.cybermanager.application.usecases.customer.GetCustomerDetailsUseCase;
import com.cybermanager.application.usecases.customer.ReattachDebtToSessionUseCase;
import com.cybermanager.application.usecases.customer.SearchOpenDebtsUseCase;
import com.cybermanager.application.usecases.customer.SearchCustomersUseCase;
import com.cybermanager.application.usecases.customer.SettleDebtUseCase;
import com.cybermanager.application.usecases.customer.UpdateCustomerUseCase;
import com.cybermanager.application.views.customer.ConversionView;
import com.cybermanager.application.views.customer.CustomerDebtSummaryView;
import com.cybermanager.application.views.customer.CustomerDebtView;
import com.cybermanager.application.views.customer.CustomerDetailsView;
import com.cybermanager.application.views.customer.CustomerSaleView;
import com.cybermanager.application.views.customer.CustomerView;
import com.cybermanager.domain.model.customer.Customer;
import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.customer.CustomerType;
import com.cybermanager.domain.model.customer.DebtId;
import com.cybermanager.domain.model.customer.DebtRecord;
import com.cybermanager.domain.model.customer.DebtStatus;
import com.cybermanager.domain.model.sales.Sale;
import com.cybermanager.domain.model.sales.SaleLine;
import com.cybermanager.domain.model.sales.SaleType;
import com.cybermanager.domain.model.session.SessionId;
import com.cybermanager.domain.model.subscription.SubscriptionOfferId;
import com.cybermanager.application.services.shared.ActorSupport;
import com.cybermanager.domain.port.customer.CustomerRepository;
import com.cybermanager.domain.port.customer.DebtRepository;
import com.cybermanager.domain.port.sales.SaleRepository;
import com.cybermanager.domain.port.session.CafeSessionRepository;
import com.cybermanager.domain.port.subscription.SubscriptionOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerApplicationService implements
        CreateCustomerUseCase,
        UpdateCustomerUseCase,
        SearchCustomersUseCase,
        GetCustomerDetailsUseCase,
        ConvertCustomerToSubscriberUseCase,
        CreateDebtFromSaleUseCase,
        ReattachDebtToSessionUseCase,
        SearchOpenDebtsUseCase,
        SettleDebtUseCase {
    private final CustomerRepository customerRepository;
    private final SubscriptionOfferRepository subscriptionOfferRepository;
    private final SaleRepository saleRepository;
    private final DebtRepository debtRepository;
    private final CafeSessionRepository sessionRepository;

    public CustomerApplicationService(
            CustomerRepository customerRepository,
            SubscriptionOfferRepository subscriptionOfferRepository,
            SaleRepository saleRepository,
            DebtRepository debtRepository,
            CafeSessionRepository sessionRepository
    ) {
        this.customerRepository = customerRepository;
        this.subscriptionOfferRepository = subscriptionOfferRepository;
        this.saleRepository = saleRepository;
        this.debtRepository = debtRepository;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public CustomerView execute(CreateCustomerCommand command) {
        Customer created;
        if ("SUBSCRIBER".equalsIgnoreCase(command.type())) {
            if (command.subscriptionOfferId() == null) {
                throw new BusinessException(BusinessErrorType.VALIDATION, "SUBSCRIPTION_OFFER_REQUIRED", "Subscription offer is required");
            }
            var offer = subscriptionOfferRepository.findById(new SubscriptionOfferId(command.subscriptionOfferId()))
                    .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SUBSCRIPTION_OFFER_NOT_FOUND", "Subscription offer not found"));
            created = customerRepository.save(Customer.createWalkIn(command.name()).convertToSubscriber(offer.includedMinutes()));
            saleRepository.save(Sale.create(
                    created.id(),
                    SaleType.SUBSCRIPTION,
                    LocalDateTime.now(),
                    List.of(new SaleLine(offer.name(), 1, offer.price(), offer.price())),
                    offer.price()
            ));
            return toView(created);
        }
        created = customerRepository.save(Customer.createWalkIn(command.name()));
        return toView(created);
    }

    @Override
    public CustomerView execute(UpdateCustomerCommand command) {
        var customer = customerRepository.findById(new CustomerId(command.customerId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
        return toView(customerRepository.save(customer.updateName(command.name())));
    }

    @Transactional(readOnly = true)
    @Override
    public List<CustomerView> execute(SearchCustomersQuery query) {
        CustomerType type = query.type() == null || query.type().isBlank() ? null : CustomerType.valueOf(query.type().toUpperCase());
        return customerRepository.search(query.term(), type).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public CustomerDetailsView execute(GetCustomerDetailsQuery query) {
        var customer = customerRepository.findById(new CustomerId(query.customerId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
        var sales = saleRepository.findByCustomerId(customer.id()).stream()
                .collect(Collectors.toMap(Sale::id, Function.identity(), (left, right) -> left, LinkedHashMap::new))
                .values()
                .stream()
                .toList();
        String currentSubscription = sales.stream()
                .filter(sale -> sale.type() == SaleType.SUBSCRIPTION)
                .findFirst()
                .map(sale -> sale.lines().isEmpty() ? "Abonnement actif" : sale.lines().getFirst().label())
                .orElse(null);

        return new CustomerDetailsView(
                customer.id().value(),
                customer.name(),
                customer.type().name(),
                customer.status().name(),
                customer.remainingMinutes(),
                currentSubscription,
                sales.stream().map(this::toSaleView).toList(),
                debtRepository.findByCustomerId(customer.id()).stream().map(this::toDebtView).toList()
        );
    }

    @Transactional(readOnly = true)
    @Override
    public List<CustomerDebtSummaryView> execute(SearchOpenDebtsQuery query) {
        Map<CustomerId, List<DebtRecord>> debtsByCustomer = debtRepository.findOpenDebts().stream()
                .collect(Collectors.groupingBy(DebtRecord::customerId));
        return debtsByCustomer.entrySet().stream()
                .map(entry -> {
                    Customer customer = customerRepository.findById(entry.getKey())
                            .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
                    var total = entry.getValue().stream()
                            .map(DebtRecord::amount)
                            .reduce(com.cybermanager.domain.model.shared.Money.of("0"), com.cybermanager.domain.model.shared.Money::add);
                    return new CustomerDebtSummaryView(
                            customer.id().value(),
                            customer.name(),
                            customer.type().name(),
                            total.amount(),
                            entry.getValue().stream().map(this::toDebtView).toList()
                    );
                })
                .sorted((left, right) -> right.totalOpenDebt().compareTo(left.totalOpenDebt()))
                .toList();
    }

    @Override
    public void execute(SettleDebtCommand command) {
        var debt = debtRepository.findById(new DebtId(command.debtId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "DEBT_NOT_FOUND", "Debt not found"));
        debtRepository.save(debt.settle(LocalDateTime.now(), command.comment()));
    }

    @Override
    public void execute(CreateDebtFromSaleCommand command) {
        var sale = saleRepository.findById(new com.cybermanager.domain.model.sales.SaleId(command.saleId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SALE_NOT_FOUND", "Sale not found"));
        var existingOpenDebt = debtRepository.findByCustomerId(sale.customerId()).stream()
                .anyMatch(debt -> debt.status() == DebtStatus.OPEN
                        && debt.label().equals(debtLabelForSale(sale))
                        && debt.amount().amount().compareTo(sale.totalAmount().amount()) == 0);
        if (existingOpenDebt) {
            throw new BusinessException(BusinessErrorType.CONFLICT, "SALE_ALREADY_IN_DEBT", "Sale is already in debt");
        }
        debtRepository.save(DebtRecord.create(sale.customerId(), debtLabelForSale(sale), sale.totalAmount(), LocalDateTime.now()));
    }

    @Override
    public void execute(ReattachDebtToSessionCommand command) {
        var debt = debtRepository.findById(new DebtId(command.debtId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "DEBT_NOT_FOUND", "Debt not found"));
        if (debt.status() != DebtStatus.OPEN) {
            throw new BusinessException(BusinessErrorType.CONFLICT, "DEBT_ALREADY_SETTLED", "Debt is already settled");
        }
        var session = sessionRepository.findById(new SessionId(command.sessionId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SESSION_NOT_FOUND", "Session not found"));
        if (!session.customerId().equals(debt.customerId())) {
            throw new BusinessException(BusinessErrorType.CONFLICT, "SESSION_CUSTOMER_MISMATCH", "Debt does not belong to the same customer");
        }
        if (session.endedAt() != null && session.paid()) {
            throw new BusinessException(BusinessErrorType.CONFLICT, "SESSION_ALREADY_PAID", "Cannot reattach debt to a paid session");
        }

        debtRepository.save(debt.settle(LocalDateTime.now(), "Reintegree a la session " + session.id().value()));
        var existingSale = saleRepository.findByCustomerId(debt.customerId()).stream()
                .filter(sale -> debtLabelForSale(sale).equals(debt.label()))
                .filter(sale -> sale.totalAmount().amount().compareTo(debt.amount().amount()) == 0)
                .findFirst();

        if (existingSale.isPresent()) {
            var sale = existingSale.get();
            saleRepository.save(new Sale(
                    sale.id(),
                    sale.customerId(),
                    session.id().value(),
                    sale.type(),
                    sale.soldAt(),
                    sale.lines(),
                    sale.totalAmount()
            ));
            return;
        }

        var soldAt = LocalDateTime.now();
        saleRepository.save(Sale.create(
                debt.customerId(),
                session.id().value(),
                saleTypeForDebtLabel(debt.label()),
                soldAt,
                saleLinesForDebt(debt),
                debt.amount()
        ));
    }

    @Override
    public ConversionView execute(ConvertCustomerToSubscriberCommand command) {
        var customer = customerRepository.findById(new CustomerId(command.customerId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found"));
        var offer = subscriptionOfferRepository.findById(new SubscriptionOfferId(command.subscriptionOfferId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "SUBSCRIPTION_OFFER_NOT_FOUND", "Subscription offer not found"));
        int deductedMinutes = command.deductCurrentSession() ? 30 : 0;
        var converted = customerRepository.save(customer.convertToSubscriber(Math.max(0, offer.includedMinutes() - deductedMinutes)));
        var sale = saleRepository.save(Sale.create(
                converted.id(),
                command.sessionId(),
                SaleType.SUBSCRIPTION,
                LocalDateTime.now(),
                List.of(new SaleLine(offer.name(), 1, offer.price(), offer.price())),
                offer.price()
        ));
        return new ConversionView(toView(converted), sale.id().value(), deductedMinutes);
    }

    private CustomerView toView(Customer customer) {
        var totalDebt = debtRepository.findByCustomerId(customer.id()).stream()
                .filter(debt -> debt.status() == DebtStatus.OPEN)
                .map(DebtRecord::amount)
                .reduce(com.cybermanager.domain.model.shared.Money.of("0"), com.cybermanager.domain.model.shared.Money::add);
        return new CustomerView(customer.id().value(), customer.name(), customer.type().name(), customer.status().name(), customer.remainingMinutes(), totalDebt.amount());
    }

    private CustomerSaleView toSaleView(Sale sale) {
        String debtLabel = debtLabelForSale(sale);
        String label = sale.lines().isEmpty() ? sale.type().name() : sale.lines().stream().map(SaleLine::label).reduce((first, second) -> first + ", " + second).orElse(sale.type().name());
        boolean openDebt = debtRepository.findByCustomerId(sale.customerId()).stream()
                .anyMatch(debt -> debt.status() == DebtStatus.OPEN
                        && debt.label().equals(debtLabel)
                        && debt.amount().amount().compareTo(sale.totalAmount().amount()) == 0);
        return new CustomerSaleView(sale.id().value(), sale.sessionId(), sale.type().name(), label, debtLabel, sale.soldAt(), sale.totalAmount().amount(), openDebt);
    }

    private String debtLabelForSale(Sale sale) {
        return switch (sale.type()) {
            case PRODUCTS -> sale.lines().stream()
                    .map(line -> line.quantity() + " x " + line.label())
                    .reduce((left, right) -> left + ", " + right)
                    .map(label -> label + " du " + com.cybermanager.application.services.shared.DateTimeLabelFormatter.format(sale.soldAt()))
                    .orElse("Vente produits du " + com.cybermanager.application.services.shared.DateTimeLabelFormatter.format(sale.soldAt()));
            case SUBSCRIPTION -> "Vente abonnement du " + com.cybermanager.application.services.shared.DateTimeLabelFormatter.format(sale.soldAt());
            case CONNECTION_TIME -> "Vente temps du " + com.cybermanager.application.services.shared.DateTimeLabelFormatter.format(sale.soldAt());
        };
    }

    private SaleType saleTypeForDebtLabel(String debtLabel) {
        if (debtLabel.startsWith("Vente abonnement du ")) {
            return SaleType.SUBSCRIPTION;
        }
        if (debtLabel.startsWith("Vente temps du ") || debtLabel.startsWith("Session du ")) {
            return SaleType.CONNECTION_TIME;
        }
        return SaleType.PRODUCTS;
    }

    private List<SaleLine> saleLinesForDebt(DebtRecord debt) {
        if (debt.label().startsWith("Vente abonnement du ")) {
            return List.of(new SaleLine("Abonnement", 1, debt.amount(), debt.amount()));
        }
        if (debt.label().startsWith("Vente temps du ") || debt.label().startsWith("Session du ")) {
            return List.of(new SaleLine("Temps de connexion", 1, debt.amount(), debt.amount()));
        }

        String label = debt.label();
        int dateIndex = label.lastIndexOf(" du ");
        String productLabel = dateIndex > 0 ? label.substring(0, dateIndex) : label;
        return List.of(new SaleLine(productLabel, 1, debt.amount(), debt.amount()));
    }

    private CustomerDebtView toDebtView(DebtRecord debtRecord) {
        return new CustomerDebtView(
                debtRecord.id().value(),
                debtRecord.label(),
                debtRecord.comment(),
                debtRecord.amount().amount(),
                debtRecord.status().name(),
                debtRecord.createdAt(),
                debtRecord.settledAt()
        );
    }
}
