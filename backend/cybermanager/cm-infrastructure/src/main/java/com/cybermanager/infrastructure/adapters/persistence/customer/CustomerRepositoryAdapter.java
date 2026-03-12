package com.cybermanager.infrastructure.adapters.persistence.customer;

import com.cybermanager.domain.model.customer.Customer;
import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.customer.CustomerStatus;
import com.cybermanager.domain.model.customer.CustomerType;
import com.cybermanager.domain.port.customer.CustomerRepository;
import com.cybermanager.infrastructure.entities.persistence.customer.CustomerJpaEntity;
import com.cybermanager.infrastructure.repositories.persistence.customer.CustomerJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CustomerRepositoryAdapter implements CustomerRepository {
    private final CustomerJpaRepository repository;

    public CustomerRepositoryAdapter(CustomerJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Customer save(Customer customer) {
        CustomerJpaEntity entity = new CustomerJpaEntity();
        entity.id = customer.id().value();
        entity.name = customer.name();
        entity.type = customer.type().name();
        entity.status = customer.status().name();
        entity.remainingMinutes = customer.remainingMinutes();
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Customer> findById(CustomerId customerId) {
        return repository.findById(customerId.value()).map(this::toDomain);
    }

    @Override
    public List<Customer> search(String term, CustomerType type) {
        String search = term == null ? "" : term.toLowerCase();
        return repository.findAll().stream()
                .filter(item -> search.isBlank() || item.name.toLowerCase().contains(search))
                .filter(item -> type == null || item.type.equals(type.name()))
                .map(this::toDomain)
                .toList();
    }

    private Customer toDomain(CustomerJpaEntity entity) {
        return new Customer(new CustomerId(entity.id), entity.name, CustomerType.valueOf(entity.type), CustomerStatus.valueOf(entity.status), entity.remainingMinutes);
    }
}

