package com.cybermanager.application.services.sales;

import com.cybermanager.application.queries.sales.GetInvoiceDetailQuery;
import com.cybermanager.application.queries.sales.SearchInvoicesQuery;
import com.cybermanager.application.services.shared.BusinessException;
import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.sales.Invoice;
import com.cybermanager.domain.model.sales.InvoiceLine;
import com.cybermanager.domain.model.sales.InvoiceStatus;
import com.cybermanager.domain.model.shared.Money;
import com.cybermanager.domain.port.customer.CustomerRepository;
import com.cybermanager.domain.port.sales.InvoiceRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvoiceApplicationServiceTest {
    @Test
    void shouldSearchInvoicesAndMapSummaryView() {
        InvoiceRepository repository = mock(InvoiceRepository.class);
        InvoiceApplicationService service = service(repository);
        Invoice invoice = invoice();

        when(repository.search("FAC-001", "Nadia", InvoiceStatus.ISSUED, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 14)))
                .thenReturn(List.of(invoice));

        var result = service.execute(new SearchInvoicesQuery("FAC-001", "Nadia", "issued", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 14)));

        assertEquals(1, result.size());
        assertEquals("FAC-001", result.getFirst().invoiceNumber());
        assertEquals(invoice.totalAmount().amount(), result.getFirst().totalAmount());
    }

    @Test
    void shouldReturnInvoiceDetail() {
        InvoiceRepository repository = mock(InvoiceRepository.class);
        InvoiceApplicationService service = service(repository);
        Invoice invoice = invoice();

        when(repository.findById(invoice.id())).thenReturn(Optional.of(invoice));

        var result = service.execute(new GetInvoiceDetailQuery(invoice.id()));

        assertEquals(invoice.invoiceNumber(), result.invoiceNumber());
        assertEquals(2, result.lines().getFirst().quantity());
    }

    @Test
    void shouldFailWhenInvoiceIsMissing() {
        InvoiceRepository repository = mock(InvoiceRepository.class);
        InvoiceApplicationService service = service(repository);
        UUID invoiceId = UUID.randomUUID();

        when(repository.findById(invoiceId)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.execute(new GetInvoiceDetailQuery(invoiceId)));
    }

    private Invoice invoice() {
        return new Invoice(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "FAC-001",
                new CustomerId(UUID.randomUUID()),
                "Nadia Benali",
                LocalDateTime.of(2026, 3, 12, 14, 30),
                InvoiceStatus.ISSUED,
                Money.of("9.50"),
                List.of(new InvoiceLine("Connexion 2h", 2, Money.of("2.50"), Money.of("5.00")))
        );
    }

    private InvoiceApplicationService service(InvoiceRepository repository) {
        return new InvoiceApplicationService(repository, mock(CustomerRepository.class), mock(InvoicePdfRenderer.class), mock(InvoiceNumberGenerator.class));
    }
}
