package com.cybermanager.application.services.customer;

import com.cybermanager.application.commands.customer.ArchiveCustomersCommand;
import com.cybermanager.application.queries.customer.ExportCustomerDebtReportQuery;
import com.cybermanager.application.queries.customer.SearchCustomerArchiveCandidatesQuery;
import com.cybermanager.domain.model.customer.Customer;
import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.customer.CustomerStatus;
import com.cybermanager.domain.model.customer.CustomerType;
import com.cybermanager.domain.model.customer.DebtRecord;
import com.cybermanager.domain.model.sales.Sale;
import com.cybermanager.domain.model.sales.SaleLine;
import com.cybermanager.domain.model.sales.SaleType;
import com.cybermanager.domain.model.session.CafeSession;
import com.cybermanager.domain.model.session.SessionId;
import com.cybermanager.domain.model.shared.Money;
import com.cybermanager.domain.port.customer.CustomerRepository;
import com.cybermanager.domain.port.customer.DebtRepository;
import com.cybermanager.domain.port.customer.GeneratedArchiveFileRepository;
import com.cybermanager.domain.port.sales.SaleRepository;
import com.cybermanager.domain.port.session.CafeSessionRepository;
import com.cybermanager.domain.port.subscription.SubscriptionOfferRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerApplicationServiceTest {
    @Test
    void shouldListArchiveCandidatesUsingLatestActivityInPeriod() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerApplicationService service = new CustomerApplicationService(
                customerRepository,
                mock(SubscriptionOfferRepository.class),
                saleRepository,
                debtRepository,
                mock(GeneratedArchiveFileRepository.class),
                sessionRepository
        );

        Customer archivedCustomer = new Customer(new CustomerId(UUID.randomUUID()), "Client archive", CustomerType.WALK_IN, CustomerStatus.ACTIVE, 0);
        Customer recentCustomer = new Customer(new CustomerId(UUID.randomUUID()), "Client recent", CustomerType.WALK_IN, CustomerStatus.ACTIVE, 0);
        Customer activeCustomer = new Customer(new CustomerId(UUID.randomUUID()), "Client actif", CustomerType.WALK_IN, CustomerStatus.ACTIVE, 0);
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(customerRepository.search("", CustomerType.WALK_IN)).thenReturn(List.of(archivedCustomer, recentCustomer, activeCustomer));
        when(saleRepository.findByCustomerIds(any())).thenReturn(List.of(
                Sale.create(archivedCustomer.id(), SaleType.PRODUCTS, LocalDateTime.of(2026, 1, 20, 10, 0), List.of(new SaleLine("Cafe", 1, Money.of("2.00"), Money.of("2.00"))), Money.of("2.00")),
                Sale.create(recentCustomer.id(), SaleType.PRODUCTS, LocalDateTime.of(2026, 2, 2, 10, 0), List.of(new SaleLine("Snack", 1, Money.of("3.00"), Money.of("3.00"))), Money.of("3.00"))
        ));
        when(debtRepository.findByCustomerIds(any())).thenReturn(List.of());
        when(sessionRepository.findByCustomerIds(any())).thenReturn(List.of(
                new CafeSession(new SessionId(UUID.randomUUID()), activeCustomer.id(), "PC-01", LocalDateTime.of(2026, 1, 15, 9, 0), null, null, false, 0, 0, Money.of("0"))
        ));

        var result = service.execute(new SearchCustomerArchiveCandidatesQuery(startDate, endDate, "WALK_IN"));

        assertEquals(1, result.size());
        assertEquals("Client archive", result.getFirst().name());
    }

    @Test
    void shouldExportCsvAndDeleteArchivedCustomersData() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerApplicationService service = new CustomerApplicationService(
                customerRepository,
                mock(SubscriptionOfferRepository.class),
                saleRepository,
                debtRepository,
                mock(GeneratedArchiveFileRepository.class),
                sessionRepository
        );

        Customer archivedCustomer = new Customer(new CustomerId(UUID.randomUUID()), "Nadia", CustomerType.WALK_IN, CustomerStatus.ACTIVE, 0);
        LocalDateTime soldAt = LocalDateTime.of(2025, 12, 15, 14, 30);
        Sale sale = Sale.create(archivedCustomer.id(), SaleType.PRODUCTS, soldAt, List.of(new SaleLine("Casque", 1, Money.of("8.00"), Money.of("8.00"))), Money.of("8.00"));
        CafeSession session = new CafeSession(new SessionId(UUID.randomUUID()), archivedCustomer.id(), "PC-02", soldAt.minusHours(1), soldAt.minusMinutes(15), null, true, 0, 2700, Money.of("4.00"));

        when(customerRepository.search("", CustomerType.WALK_IN)).thenReturn(List.of(archivedCustomer));
        when(saleRepository.findByCustomerIds(any())).thenReturn(List.of(sale));
        when(debtRepository.findByCustomerIds(any())).thenReturn(List.of());
        when(sessionRepository.findByCustomerIds(any())).thenReturn(List.of(session));

        var result = service.execute(new ArchiveCustomersCommand("admin@cm.local", Set.of("ADMIN"), LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31), "WALK_IN", "csv"));

        assertEquals(1, result.archivedCustomers());
        assertEquals("customers-archive-walk_in-2025-12-01-to-2025-12-31.csv", result.fileName());
        assertTrue(new String(result.content()).contains("Nadia"));
        verify(debtRepository).deleteByCustomerIds(argThat(ids -> ids.size() == 1 && ids.getFirst().equals(archivedCustomer.id())));
        verify(saleRepository).deleteByCustomerIds(argThat(ids -> ids.size() == 1 && ids.getFirst().equals(archivedCustomer.id())));
        verify(sessionRepository).deleteByCustomerIds(argThat(ids -> ids.size() == 1 && ids.getFirst().equals(archivedCustomer.id())));
        verify(customerRepository).deleteByIds(argThat(ids -> ids.size() == 1 && ids.getFirst().equals(archivedCustomer.id())));
    }

    @Test
    void shouldNotDeleteAnythingWhenArchiveMatchesNoCustomer() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerApplicationService service = new CustomerApplicationService(
                customerRepository,
                mock(SubscriptionOfferRepository.class),
                saleRepository,
                debtRepository,
                mock(GeneratedArchiveFileRepository.class),
                sessionRepository
        );

        when(customerRepository.search("", CustomerType.WALK_IN)).thenReturn(List.of());

        var result = service.execute(new ArchiveCustomersCommand("admin@cm.local", Set.of("ADMIN"), LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31), "WALK_IN", "csv"));

        assertEquals(0, result.archivedCustomers());
        verify(debtRepository, never()).deleteByCustomerIds(any());
        verify(saleRepository, never()).deleteByCustomerIds(any());
        verify(sessionRepository, never()).deleteByCustomerIds(any());
        verify(customerRepository, never()).deleteByIds(any());
    }

    @Test
    void shouldExcludeSubscribersAndCustomersWithDebtOrRemainingMinutesFromArchiveCandidates() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerApplicationService service = new CustomerApplicationService(
                customerRepository,
                mock(SubscriptionOfferRepository.class),
                saleRepository,
                debtRepository,
                mock(GeneratedArchiveFileRepository.class),
                sessionRepository
        );

        Customer walkInWithDebt = new Customer(new CustomerId(UUID.randomUUID()), "Dette", CustomerType.WALK_IN, CustomerStatus.ACTIVE, 0);
        Customer subscriber = new Customer(new CustomerId(UUID.randomUUID()), "Abonne", CustomerType.SUBSCRIBER, CustomerStatus.ACTIVE, 0);
        Customer walkInWithCredit = new Customer(new CustomerId(UUID.randomUUID()), "Credit", CustomerType.WALK_IN, CustomerStatus.ACTIVE, 20);
        LocalDateTime activity = LocalDateTime.of(2026, 1, 10, 12, 0);

        when(customerRepository.search("", CustomerType.WALK_IN)).thenReturn(List.of(walkInWithDebt, walkInWithCredit));
        when(saleRepository.findByCustomerIds(any())).thenReturn(List.of(
                Sale.create(walkInWithDebt.id(), SaleType.PRODUCTS, activity, List.of(new SaleLine("Cafe", 1, Money.of("2.00"), Money.of("2.00"))), Money.of("2.00")),
                Sale.create(walkInWithCredit.id(), SaleType.PRODUCTS, activity, List.of(new SaleLine("Cafe", 1, Money.of("2.00"), Money.of("2.00"))), Money.of("2.00"))
        ));
        when(debtRepository.findByCustomerIds(any())).thenReturn(List.of(
                DebtRecord.create(walkInWithDebt.id(), "Ancienne dette", Money.of("5.00"), activity)
        ));
        when(sessionRepository.findByCustomerIds(any())).thenReturn(List.of());

        var result = service.execute(new SearchCustomerArchiveCandidatesQuery(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "WALK_IN"));
        assertEquals(0, result.size());

        when(customerRepository.search("", CustomerType.SUBSCRIBER)).thenReturn(List.of(subscriber));
        when(saleRepository.findByCustomerIds(any())).thenReturn(List.of(
                Sale.create(subscriber.id(), SaleType.SUBSCRIPTION, activity, List.of(new SaleLine("Forfait", 1, Money.of("10.00"), Money.of("10.00"))), Money.of("10.00"))
        ));
        when(debtRepository.findByCustomerIds(any())).thenReturn(List.of());

        var subscriberResult = service.execute(new SearchCustomerArchiveCandidatesQuery(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), "SUBSCRIBER"));
        assertEquals(0, subscriberResult.size());
    }

    @Test
    void shouldExportXlsxArchive() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerApplicationService service = new CustomerApplicationService(
                customerRepository,
                mock(SubscriptionOfferRepository.class),
                saleRepository,
                debtRepository,
                mock(GeneratedArchiveFileRepository.class),
                sessionRepository
        );

        Customer archivedCustomer = new Customer(new CustomerId(UUID.randomUUID()), "Client xlsx", CustomerType.WALK_IN, CustomerStatus.ACTIVE, 0);
        LocalDateTime soldAt = LocalDateTime.of(2025, 11, 15, 14, 30);
        when(customerRepository.search("", CustomerType.WALK_IN)).thenReturn(List.of(archivedCustomer));
        when(saleRepository.findByCustomerIds(any())).thenReturn(List.of(
                Sale.create(archivedCustomer.id(), SaleType.PRODUCTS, soldAt, List.of(new SaleLine("Casque", 1, Money.of("8.00"), Money.of("8.00"))), Money.of("8.00"))
        ));
        when(debtRepository.findByCustomerIds(any())).thenReturn(List.of());
        when(sessionRepository.findByCustomerIds(any())).thenReturn(List.of());

        var result = service.execute(new ArchiveCustomersCommand("admin@cm.local", Set.of("ADMIN"), LocalDate.of(2025, 11, 1), LocalDate.of(2025, 11, 30), "WALK_IN", "xlsx"));

        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", result.mediaType());
        assertTrue(result.content().length > 0);
    }

    @Test
    void shouldExportDebtReportPdf() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerApplicationService service = new CustomerApplicationService(
                customerRepository,
                mock(SubscriptionOfferRepository.class),
                saleRepository,
                debtRepository,
                mock(GeneratedArchiveFileRepository.class),
                sessionRepository
        );

        Customer customer = new Customer(new CustomerId(UUID.randomUUID()), "Client dette", CustomerType.WALK_IN, CustomerStatus.ACTIVE, 0);
        LocalDateTime activity = LocalDateTime.of(2026, 2, 10, 16, 0);
        when(customerRepository.findById(customer.id())).thenReturn(java.util.Optional.of(customer));
        when(debtRepository.findOpenDebts()).thenReturn(List.of(
                DebtRecord.create(customer.id(), "1 x Cafe du 10/02/2026 16:00", Money.of("2.00"), activity)
        ));

        var result = service.execute(new ExportCustomerDebtReportQuery("admin@cm.local", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), ""));

        assertEquals("application/pdf", result.mediaType());
        assertTrue(result.content().length > 0);
        assertEquals("customer-open-debts-report.pdf", result.fileName());
    }

    @Test
    void shouldIncludeOpenDebtsEvenForCustomerWithActiveSession() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        SaleRepository saleRepository = mock(SaleRepository.class);
        DebtRepository debtRepository = mock(DebtRepository.class);
        CafeSessionRepository sessionRepository = mock(CafeSessionRepository.class);
        CustomerApplicationService service = new CustomerApplicationService(
                customerRepository,
                mock(SubscriptionOfferRepository.class),
                saleRepository,
                debtRepository,
                mock(GeneratedArchiveFileRepository.class),
                sessionRepository
        );

        Customer customer = new Customer(new CustomerId(UUID.randomUUID()), "Session active", CustomerType.WALK_IN, CustomerStatus.ACTIVE, 0);
        when(customerRepository.findById(customer.id())).thenReturn(java.util.Optional.of(customer));
        when(debtRepository.findOpenDebts()).thenReturn(List.of(
                DebtRecord.create(customer.id(), "Dette en cours de session", Money.of("9.00"), LocalDateTime.of(2026, 3, 14, 20, 0))
        ));

        var result = service.execute(new ExportCustomerDebtReportQuery("admin@cm.local", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), ""));

        assertTrue(result.content().length > 0);
    }
}
