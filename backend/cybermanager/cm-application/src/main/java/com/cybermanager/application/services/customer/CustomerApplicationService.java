package com.cybermanager.application.services.customer;

import com.cybermanager.application.commands.customer.ConvertCustomerToSubscriberCommand;
import com.cybermanager.application.commands.customer.CreateCustomerCommand;
import com.cybermanager.application.commands.customer.UpdateCustomerCommand;
import com.cybermanager.application.queries.customer.GetCustomerDetailsQuery;
import com.cybermanager.application.queries.customer.SearchCustomersQuery;
import com.cybermanager.application.usecases.customer.ConvertCustomerToSubscriberUseCase;
import com.cybermanager.application.usecases.customer.CreateCustomerUseCase;
import com.cybermanager.application.usecases.customer.GetCustomerDetailsUseCase;
import com.cybermanager.application.usecases.customer.SearchCustomersUseCase;
import com.cybermanager.application.usecases.customer.UpdateCustomerUseCase;
import com.cybermanager.application.views.customer.ConversionView;
import com.cybermanager.application.views.customer.CustomerDetailsView;
import com.cybermanager.application.views.customer.CustomerSaleView;
import com.cybermanager.application.views.customer.CustomerView;
import com.cybermanager.domain.model.customer.Customer;
import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.customer.CustomerType;
import com.cybermanager.domain.model.sales.Sale;
import com.cybermanager.domain.model.sales.SaleLine;
import com.cybermanager.domain.model.sales.SaleType;
import com.cybermanager.domain.model.subscription.SubscriptionOfferId;
import com.cybermanager.domain.port.customer.CustomerRepository;
import com.cybermanager.domain.port.sales.SaleRepository;
import com.cybermanager.domain.port.subscription.SubscriptionOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CustomerApplicationService implements
        CreateCustomerUseCase,
        UpdateCustomerUseCase,
        SearchCustomersUseCase,
        GetCustomerDetailsUseCase,
        ConvertCustomerToSubscriberUseCase {
    private final CustomerRepository customerRepository;
    private final SubscriptionOfferRepository subscriptionOfferRepository;
    private final SaleRepository saleRepository;

    public CustomerApplicationService(
            CustomerRepository customerRepository,
            SubscriptionOfferRepository subscriptionOfferRepository,
            SaleRepository saleRepository
    ) {
        this.customerRepository = customerRepository;
        this.subscriptionOfferRepository = subscriptionOfferRepository;
        this.saleRepository = saleRepository;
    }

    @Override
    public CustomerView execute(CreateCustomerCommand command) {
        Customer created;
        if ("SUBSCRIBER".equalsIgnoreCase(command.type())) {
            if (command.subscriptionOfferId() == null) {
                throw new IllegalArgumentException("Subscription offer is required");
            }
            var offer = subscriptionOfferRepository.findById(new SubscriptionOfferId(command.subscriptionOfferId()))
                    .orElseThrow(() -> new IllegalArgumentException("Subscription offer not found"));
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
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
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
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
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
                sales.stream().map(this::toSaleView).toList()
        );
    }

    @Override
    public ConversionView execute(ConvertCustomerToSubscriberCommand command) {
        var customer = customerRepository.findById(new CustomerId(command.customerId()))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        var offer = subscriptionOfferRepository.findById(new SubscriptionOfferId(command.subscriptionOfferId()))
                .orElseThrow(() -> new IllegalArgumentException("Subscription offer not found"));
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
        return new CustomerView(customer.id().value(), customer.name(), customer.type().name(), customer.status().name(), customer.remainingMinutes());
    }

    private CustomerSaleView toSaleView(Sale sale) {
        String label = sale.lines().isEmpty() ? sale.type().name() : sale.lines().stream().map(SaleLine::label).reduce((first, second) -> first + ", " + second).orElse(sale.type().name());
        return new CustomerSaleView(sale.id().value(), sale.type().name(), label, sale.soldAt(), sale.totalAmount().amount());
    }
}
