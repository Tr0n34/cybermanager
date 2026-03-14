package com.cybermanager.application.services.sales;

import com.cybermanager.application.commands.sales.CancelInvoiceCommand;
import com.cybermanager.application.commands.sales.CreateManualInvoiceCommand;
import com.cybermanager.application.queries.sales.GetInvoiceDetailQuery;
import com.cybermanager.application.queries.sales.ReissueInvoicePdfQuery;
import com.cybermanager.application.queries.sales.SearchInvoicesQuery;
import com.cybermanager.application.services.shared.BusinessErrorType;
import com.cybermanager.application.services.shared.BusinessException;
import com.cybermanager.application.usecases.sales.CancelInvoiceUseCase;
import com.cybermanager.application.usecases.sales.CreateManualInvoiceUseCase;
import com.cybermanager.application.usecases.sales.GetInvoiceDetailUseCase;
import com.cybermanager.application.usecases.sales.ReissueInvoicePdfUseCase;
import com.cybermanager.application.usecases.sales.SearchInvoicesUseCase;
import com.cybermanager.application.views.sales.InvoiceDetailView;
import com.cybermanager.application.views.sales.InvoiceLineView;
import com.cybermanager.application.views.sales.InvoicePdfView;
import com.cybermanager.application.views.sales.InvoiceSummaryView;
import com.cybermanager.domain.model.customer.CustomerId;
import com.cybermanager.domain.model.sales.Invoice;
import com.cybermanager.domain.model.sales.InvoiceLine;
import com.cybermanager.domain.model.sales.InvoiceStatus;
import com.cybermanager.domain.model.shared.Money;
import com.cybermanager.domain.port.customer.CustomerRepository;
import com.cybermanager.domain.port.sales.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class InvoiceApplicationService implements SearchInvoicesUseCase, GetInvoiceDetailUseCase, CreateManualInvoiceUseCase, CancelInvoiceUseCase, ReissueInvoicePdfUseCase {
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final InvoicePdfRenderer invoicePdfRenderer;
    private final InvoiceNumberGenerator invoiceNumberGenerator;

    public InvoiceApplicationService(
            InvoiceRepository invoiceRepository,
            CustomerRepository customerRepository,
            InvoicePdfRenderer invoicePdfRenderer,
            InvoiceNumberGenerator invoiceNumberGenerator
    ) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.invoicePdfRenderer = invoicePdfRenderer;
        this.invoiceNumberGenerator = invoiceNumberGenerator;
    }

    @Transactional(readOnly = true)
    @Override
    public List<InvoiceSummaryView> execute(SearchInvoicesQuery query) {
        InvoiceStatus status = query.status() == null || query.status().isBlank() ? null : InvoiceStatus.valueOf(query.status().toUpperCase());
        return invoiceRepository.search(query.invoiceNumber(), query.customerName(), status, query.startDate(), query.endDate()).stream()
                .map(this::toSummaryView)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public InvoiceDetailView execute(GetInvoiceDetailQuery query) {
        var invoice = invoiceRepository.findById(query.invoiceId())
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found"));
        return toDetailView(invoice);
    }

    @Override
    public InvoicePdfView execute(CreateManualInvoiceCommand command) {
        String customerName = normalizeRequired(command.customerName(), "INVOICE_CUSTOMER_REQUIRED", "Le client de la facture est requis.");
        if (command.customerId() != null && customerRepository.findById(new CustomerId(command.customerId())).isEmpty()) {
            throw new BusinessException(BusinessErrorType.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found");
        }
        if (command.lines() == null || command.lines().isEmpty()) {
            throw new BusinessException(BusinessErrorType.VALIDATION, "INVOICE_LINES_REQUIRED", "Au moins une ligne de facture est requise.");
        }

        List<InvoiceLine> lines = new ArrayList<>();
        for (var line : command.lines()) {
            String label = normalizeRequired(line.label(), "INVOICE_LINE_LABEL_REQUIRED", "Le libelle de ligne est requis.");
            if (line.quantity() <= 0) {
                throw new BusinessException(BusinessErrorType.VALIDATION, "INVOICE_LINE_QUANTITY_INVALID", "La quantite de ligne doit etre positive.");
            }
            if (line.unitPrice() == null || line.unitPrice().signum() < 0) {
                throw new BusinessException(BusinessErrorType.VALIDATION, "INVOICE_LINE_PRICE_INVALID", "Le prix unitaire doit etre valide.");
            }
            Money unitPrice = new Money(line.unitPrice());
            lines.add(new InvoiceLine(label, line.quantity(), unitPrice, new Money(line.unitPrice().multiply(java.math.BigDecimal.valueOf(line.quantity())))));
        }

        Money total = lines.stream()
                .map(InvoiceLine::totalPrice)
                .reduce(Money.of("0"), Money::add);

        Invoice invoice = invoiceRepository.save(new Invoice(
                java.util.UUID.randomUUID(),
                null,
                invoiceNumberGenerator.next(),
                command.customerId() == null ? null : new CustomerId(command.customerId()),
                customerName,
                LocalDateTime.now(),
                InvoiceStatus.ISSUED,
                total,
                lines
        ));
        return toPdfView(invoice);
    }

    @Override
    public InvoiceDetailView execute(CancelInvoiceCommand command) {
        Invoice invoice = invoiceRepository.findById(command.invoiceId())
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found"));
        if (invoice.status() == InvoiceStatus.CANCELLED) {
            throw new BusinessException(BusinessErrorType.CONFLICT, "INVOICE_ALREADY_CANCELLED", "Invoice is already cancelled");
        }
        Invoice cancelled = invoiceRepository.save(new Invoice(
                invoice.id(),
                invoice.saleId(),
                invoice.invoiceNumber(),
                invoice.customerId(),
                invoice.customerName(),
                invoice.issuedAt(),
                InvoiceStatus.CANCELLED,
                invoice.totalAmount(),
                invoice.lines()
        ));
        return toDetailView(cancelled);
    }

    @Transactional(readOnly = true)
    @Override
    public InvoicePdfView execute(ReissueInvoicePdfQuery query) {
        Invoice invoice = invoiceRepository.findById(query.invoiceId())
                .orElseThrow(() -> new BusinessException(BusinessErrorType.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found"));
        return toPdfView(invoice);
    }

    private InvoiceSummaryView toSummaryView(Invoice invoice) {
        return new InvoiceSummaryView(
                invoice.id(),
                invoice.saleId(),
                invoice.invoiceNumber(),
                invoice.customerName(),
                invoice.issuedAt(),
                invoice.status().name(),
                invoice.totalAmount().amount()
        );
    }

    private InvoiceDetailView toDetailView(Invoice invoice) {
        return new InvoiceDetailView(
                invoice.id(),
                invoice.saleId(),
                invoice.invoiceNumber(),
                invoice.customerId() == null ? null : invoice.customerId().value(),
                invoice.customerName(),
                invoice.issuedAt(),
                invoice.status().name(),
                invoice.totalAmount().amount(),
                invoice.lines().stream()
                        .map(line -> new InvoiceLineView(line.label(), line.quantity(), line.unitPrice().amount(), line.totalPrice().amount()))
                        .toList()
        );
    }

    private InvoicePdfView toPdfView(Invoice invoice) {
        return new InvoicePdfView(
                invoice.id(),
                invoice.invoiceNumber(),
                invoice.invoiceNumber() + ".pdf",
                "application/pdf",
                invoicePdfRenderer.render(invoice)
        );
    }

    private String normalizeRequired(String value, String code, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new BusinessException(BusinessErrorType.VALIDATION, code, message);
        }
        return value.trim();
    }
}
