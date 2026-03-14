package com.cybermanager.api.controllers.sales;

import com.cybermanager.api.shared.ApiSupport;
import com.cybermanager.application.commands.sales.CancelInvoiceCommand;
import com.cybermanager.application.commands.sales.CreateManualInvoiceCommand;
import com.cybermanager.application.queries.sales.GetInvoiceDetailQuery;
import com.cybermanager.application.queries.sales.ReissueInvoicePdfQuery;
import com.cybermanager.application.queries.sales.SearchInvoicesQuery;
import com.cybermanager.application.usecases.sales.CancelInvoiceUseCase;
import com.cybermanager.application.usecases.sales.CreateManualInvoiceUseCase;
import com.cybermanager.application.usecases.sales.GetInvoiceDetailUseCase;
import com.cybermanager.application.usecases.sales.ReissueInvoicePdfUseCase;
import com.cybermanager.application.usecases.sales.SearchInvoicesUseCase;
import com.cybermanager.application.views.sales.InvoiceDetailView;
import com.cybermanager.application.views.sales.InvoiceSummaryView;
import com.cybermanager.infrastructure.security.users.JwtAccessTokenReader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.cybermanager.api.dtos.sales.InvoiceDtos.CreateInvoiceRequest;
import static com.cybermanager.api.dtos.sales.InvoiceDtos.InvoiceDetailResponse;
import static com.cybermanager.api.dtos.sales.InvoiceDtos.InvoiceLineResponse;
import static com.cybermanager.api.dtos.sales.InvoiceDtos.InvoiceSummaryResponse;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private final SearchInvoicesUseCase searchInvoicesUseCase;
    private final GetInvoiceDetailUseCase getInvoiceDetailUseCase;
    private final CreateManualInvoiceUseCase createManualInvoiceUseCase;
    private final CancelInvoiceUseCase cancelInvoiceUseCase;
    private final ReissueInvoicePdfUseCase reissueInvoicePdfUseCase;
    private final JwtAccessTokenReader tokenReader;

    public InvoiceController(
            SearchInvoicesUseCase searchInvoicesUseCase,
            GetInvoiceDetailUseCase getInvoiceDetailUseCase,
            CreateManualInvoiceUseCase createManualInvoiceUseCase,
            CancelInvoiceUseCase cancelInvoiceUseCase,
            ReissueInvoicePdfUseCase reissueInvoicePdfUseCase,
            JwtAccessTokenReader tokenReader
    ) {
        this.searchInvoicesUseCase = searchInvoicesUseCase;
        this.getInvoiceDetailUseCase = getInvoiceDetailUseCase;
        this.createManualInvoiceUseCase = createManualInvoiceUseCase;
        this.cancelInvoiceUseCase = cancelInvoiceUseCase;
        this.reissueInvoicePdfUseCase = reissueInvoicePdfUseCase;
        this.tokenReader = tokenReader;
    }

    @GetMapping
    public ResponseEntity<List<InvoiceSummaryResponse>> search(
            @RequestParam(name = "invoiceNumber", required = false) String invoiceNumber,
            @RequestParam(name = "customerName", required = false) String customerName,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "startDate", required = false) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) LocalDate endDate
    ) {
        return ResponseEntity.ok(searchInvoicesUseCase.execute(new SearchInvoicesQuery(invoiceNumber, customerName, status, startDate, endDate)).stream()
                .map(this::toSummaryResponse)
                .toList());
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceDetailResponse> detail(@PathVariable("invoiceId") UUID invoiceId) {
        return ResponseEntity.ok(toDetailResponse(getInvoiceDetailUseCase.execute(new GetInvoiceDetailQuery(invoiceId))));
    }

    @PostMapping
    public ResponseEntity<byte[]> create(@RequestHeader("Authorization") String authorization, @RequestBody CreateInvoiceRequest request) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        var result = createManualInvoiceUseCase.execute(new CreateManualInvoiceCommand(
                actor.email(),
                request.customerId(),
                request.customerName(),
                request.lines().stream()
                        .map(line -> new CreateManualInvoiceCommand.ManualInvoiceLineCommand(line.label(), line.quantity(), line.unitPrice()))
                        .toList()
        ));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .header("X-Invoice-Id", result.invoiceId().toString())
                .header("X-Invoice-Number", result.invoiceNumber())
                .contentType(MediaType.parseMediaType(result.mediaType()))
                .body(result.content());
    }

    @PostMapping("/{invoiceId}/cancel")
    public ResponseEntity<InvoiceDetailResponse> cancel(@PathVariable("invoiceId") UUID invoiceId) {
        return ResponseEntity.ok(toDetailResponse(cancelInvoiceUseCase.execute(new CancelInvoiceCommand(invoiceId))));
    }

    @GetMapping("/{invoiceId}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable("invoiceId") UUID invoiceId) {
        var result = reissueInvoicePdfUseCase.execute(new ReissueInvoicePdfQuery(invoiceId));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .header("X-Invoice-Id", result.invoiceId().toString())
                .header("X-Invoice-Number", result.invoiceNumber())
                .contentType(MediaType.parseMediaType(result.mediaType()))
                .body(result.content());
    }

    private InvoiceSummaryResponse toSummaryResponse(InvoiceSummaryView view) {
        return new InvoiceSummaryResponse(
                view.invoiceId(),
                view.saleId(),
                view.invoiceNumber(),
                view.customerName(),
                view.issuedAt(),
                view.status(),
                view.totalAmount()
        );
    }

    private InvoiceDetailResponse toDetailResponse(InvoiceDetailView view) {
        return new InvoiceDetailResponse(
                view.invoiceId(),
                view.saleId(),
                view.customerId(),
                view.invoiceNumber(),
                view.customerName(),
                view.issuedAt(),
                view.status(),
                view.totalAmount(),
                view.lines().stream()
                        .map(line -> new InvoiceLineResponse(line.label(), line.quantity(), line.unitPrice(), line.totalPrice()))
                        .toList()
        );
    }
}
