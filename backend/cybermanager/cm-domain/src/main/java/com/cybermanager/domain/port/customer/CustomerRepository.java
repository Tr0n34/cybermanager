package com.cybermanager.domain.port.customer;

import com.cybermanager.domain.model.customer.Customer;
import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.customer.CustomerType;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(CustomerId customerId);
    void deleteByIds(List<CustomerId> customerIds);
    List<Customer> search(String term, CustomerType type);
}

