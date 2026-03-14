package com.cybermanager.domain.port.sales;

import com.cybermanager.domain.model.sales.Invoice;
import com.cybermanager.domain.model.sales.InvoiceStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository {
    Invoice save(Invoice invoice);
    List<Invoice> search(String invoiceNumber, String customerName, InvoiceStatus status, LocalDate startDate, LocalDate endDate);
    Optional<Invoice> findById(UUID invoiceId);
}
