package com.cybermanager.domain.port.customer;

import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.customer.DebtId;
import com.cybermanager.domain.model.customer.DebtRecord;

import java.util.List;
import java.util.Optional;

public interface DebtRepository {
    DebtRecord save(DebtRecord debtRecord);
    Optional<DebtRecord> findById(DebtId debtId);
    void deleteById(DebtId debtId);
    void deleteByCustomerIds(List<CustomerId> customerIds);
    List<DebtRecord> findByCustomerId(CustomerId customerId);
    List<DebtRecord> findByCustomerIds(List<CustomerId> customerIds);
    List<DebtRecord> findOpenDebts();
}
