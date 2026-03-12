package com.cybermanager.application.services.customer;

import com.cybermanager.application.commands.customer.ConvertCustomerToSubscriberCommand;
import com.cybermanager.application.commands.customer.CreateCustomerCommand;
import com.cybermanager.application.commands.customer.SettleDebtCommand;
import com.cybermanager.application.commands.customer.UpdateCustomerCommand;
import com.cybermanager.application.services.shared.BusinessErrorType;
import com.cybermanager.application.services.shared.BusinessException;
import com.cybermanager.application.queries.customer.GetCustomerDetailsQuery;
import com.cybermanager.application.queries.customer.SearchOpenDebtsQuery;
import com.cybermanager.application.queries.customer.SearchCustomersQuery;
import com.cybermanager.application.usecases.customer.ConvertCustomerToSubscriberUseCase;
import com.cybermanager.application.usecases.customer.CreateCustomerUseCase;
import com.cybermanager.application.usecases.customer.GetCustomerDetailsUseCase;
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
import com.cybermanager.domain.model.subscription.SubscriptionOfferId;
import com.cybermanager.application.services.shared.ActorSupport;
import com.cybermanager.domain.port.customer.CustomerRepository;
import com.cybermanager.domain.port.customer.DebtRepository;
import com.cybermanager.domain.port.sales.SaleRepository;
import com.cybermanager.domain.port.subscription.SubscriptionOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerApplicationService implements
        CreateCustomerUseCase,
        UpdateCustomerUseCase,
        SearchCustomersUseCase,
        GetCustomerDetailsUseCase,
        ConvertCustomerToSubscriberUseCase,
        SearchOpenDebtsUseCase,
        SettleDebtUseCase {
    private final CustomerRepository customerRepository;
    private final SubscriptionOfferRepository subscriptionOfferRepository;
    private final SaleRepository saleRepository;
    private final DebtRepository debtRepository;

    public CustomerApplicationService(
            CustomerRepository customerRepository,
            SubscriptionOfferRepository subscriptionOfferRepository,
            SaleRepository saleRepository,
            DebtRepository debtRepository
    ) {
        this.customerRepository = customerRepository;
        this.subscriptionOfferRepository = subscriptionOfferRepository;
        this.saleRepository = saleRepository;
        this.debtRepository = debtRepository;
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
        var sales = saleRepository.findByCustomerId(customer.id());
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
        ActorSupport.requireAdmin(command.actorRoles());
        var debt = debtRepository.findById(new DebtId(command.debtId()))
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "DEBT_NOT_FOUND", "Debt not found"));
        debtRepository.save(debt.settle(LocalDateTime.now()));
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
        String label = sale.lines().isEmpty() ? sale.type().name() : sale.lines().stream().map(SaleLine::label).reduce((first, second) -> first + ", " + second).orElse(sale.type().name());
        return new CustomerSaleView(sale.id().value(), sale.type().name(), label, sale.soldAt(), sale.totalAmount().amount());
    }

    private CustomerDebtView toDebtView(DebtRecord debtRecord) {
        return new CustomerDebtView(
                debtRecord.id().value(),
                debtRecord.label(),
                debtRecord.amount().amount(),
                debtRecord.status().name(),
                debtRecord.createdAt(),
                debtRecord.settledAt()
        );
    }
}
