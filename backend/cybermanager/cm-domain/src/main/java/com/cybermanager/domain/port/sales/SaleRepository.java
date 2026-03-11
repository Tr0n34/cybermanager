package com.cybermanager.domain.port.sales;

import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.sales.Sale;
import com.cybermanager.domain.model.sales.SaleId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SaleRepository {
    Sale save(Sale sale);
    Optional<Sale> findById(SaleId saleId);
    List<Sale> findByDay(LocalDate date);
    List<Sale> findByCustomerId(CustomerId customerId);
}
