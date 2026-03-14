package com.cybermanager.api.controllers.customer;

import com.cybermanager.api.dtos.customer.CustomerDtos.*;
import com.cybermanager.api.shared.ApiSupport;
import com.cybermanager.application.commands.customer.ConvertCustomerToSubscriberCommand;
import com.cybermanager.application.commands.customer.CreateDebtFromSaleCommand;
import com.cybermanager.application.commands.customer.CreateCustomerCommand;
import com.cybermanager.application.commands.customer.ArchiveCustomersCommand;
import com.cybermanager.application.commands.customer.ReattachDebtToSessionCommand;
import com.cybermanager.application.commands.customer.SettleDebtCommand;
import com.cybermanager.application.commands.customer.UpdateCustomerCommand;
import com.cybermanager.application.queries.customer.GetCustomerDetailsQuery;
import com.cybermanager.application.queries.customer.ExportCustomerDebtReportQuery;
import com.cybermanager.application.queries.customer.ListGeneratedArchiveFilesQuery;
import com.cybermanager.application.queries.customer.SearchCustomerArchiveCandidatesQuery;
import com.cybermanager.application.queries.customer.SearchOpenDebtsQuery;
import com.cybermanager.application.queries.customer.SearchCustomersQuery;
import com.cybermanager.application.services.customer.CustomerApplicationService;
import com.cybermanager.infrastructure.security.users.JwtAccessTokenReader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.cybermanager.api.dtos.customer.GeneratedArchiveFileResponse;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerApplicationService service;
    private final JwtAccessTokenReader tokenReader;

    public CustomerController(CustomerApplicationService service, JwtAccessTokenReader tokenReader) {
        this.service = service;
        this.tokenReader = tokenReader;
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> search(
            @RequestParam(name = "term", required = false) String term,
            @RequestParam(name = "type", required = false) String type
    ) {
        return ResponseEntity.ok(service.execute(new SearchCustomersQuery(term, type)).stream()
                .map(view -> new CustomerResponse(view.customerId(), view.name(), view.type(), view.status(), view.remainingMinutes(), view.openDebtAmount()))
                .toList());
    }

    @GetMapping("/archive/candidates")
    public ResponseEntity<List<ArchiveCustomerCandidateResponse>> archiveCandidates(
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate,
            @RequestParam(name = "type", required = false) String type
    ) {
        return ResponseEntity.ok(service.execute(new SearchCustomerArchiveCandidatesQuery(startDate, endDate, type)).stream()
                .map(candidate -> new ArchiveCustomerCandidateResponse(
                        candidate.customerId(),
                        candidate.name(),
                        candidate.type(),
                        candidate.status(),
                        candidate.remainingMinutes(),
                        candidate.latestActivityAt(),
                        candidate.sessionCount(),
                        candidate.saleCount(),
                        candidate.debtCount(),
                        candidate.salesTotal(),
                        candidate.debtTotal()
                ))
                .toList());
    }

    @GetMapping("/archive/files")
    public ResponseEntity<List<GeneratedArchiveFileResponse>> generatedArchiveFiles() {
        return ResponseEntity.ok(service.execute(new ListGeneratedArchiveFilesQuery()).stream()
                .map(file -> new GeneratedArchiveFileResponse(file.id(), file.fileName(), file.fileType(), file.generatedBy(), file.generatedAt()))
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDetailsResponse> get(@PathVariable("id") UUID id) {
        var view = service.execute(new GetCustomerDetailsQuery(id));
        return ResponseEntity.ok(new CustomerDetailsResponse(
                view.customerId(),
                view.name(),
                view.type(),
                view.status(),
                view.remainingMinutes(),
                view.currentSubscriptionLabel(),
                view.purchases().stream().map(purchase -> new CustomerPurchaseResponse(
                        purchase.saleId(),
                        purchase.sessionId(),
                        purchase.type(),
                        purchase.label(),
                        purchase.debtLabel(),
                        purchase.soldAt(),
                        purchase.totalAmount(),
                        purchase.openDebt()
                )).toList(),
                view.debts().stream().map(debt -> new CustomerDebtResponse(
                        debt.debtId(),
                        debt.label(),
                        debt.comment(),
                        debt.amount(),
                        debt.status(),
                        debt.createdAt(),
                        debt.settledAt()
                )).toList()
        ));
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@RequestHeader("Authorization") String authorization, @RequestBody CustomerRequest request) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        var view = service.execute(new CreateCustomerCommand(actor.email(), actor.roles(), request.name(), request.type(), request.subscriptionOfferId()));
        return ResponseEntity.ok(new CustomerResponse(view.customerId(), view.name(), view.type(), view.status(), view.remainingMinutes(), view.openDebtAmount()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> update(@RequestHeader("Authorization") String authorization, @PathVariable("id") UUID id, @RequestBody CustomerRequest request) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        var view = service.execute(new UpdateCustomerCommand(actor.email(), actor.roles(), id, request.name()));
        return ResponseEntity.ok(new CustomerResponse(view.customerId(), view.name(), view.type(), view.status(), view.remainingMinutes(), view.openDebtAmount()));
    }

    @PostMapping("/{id}/convert-to-subscriber")
    public ResponseEntity<ConversionResponse> convert(@RequestHeader("Authorization") String authorization, @PathVariable("id") UUID id, @RequestBody ConvertToSubscriberRequest request) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        var view = service.execute(new ConvertCustomerToSubscriberCommand(actor.email(), actor.roles(), id, request.subscriptionOfferId(), request.deductCurrentSession(), request.sessionId()));
        var customer = view.customer();
        return ResponseEntity.ok(new ConversionResponse(new CustomerResponse(customer.customerId(), customer.name(), customer.type(), customer.status(), customer.remainingMinutes(), customer.openDebtAmount()), view.saleId(), view.deductedMinutes()));
    }

    @GetMapping("/debts")
    public ResponseEntity<List<DebtCustomerResponse>> debts() {
        return ResponseEntity.ok(service.execute(new SearchOpenDebtsQuery()).stream()
                .map(customer -> new DebtCustomerResponse(
                        customer.customerId(),
                        customer.customerName(),
                        customer.customerType(),
                        customer.totalOpenDebt(),
                        customer.debts().stream().map(debt -> new CustomerDebtResponse(
                                debt.debtId(),
                                debt.label(),
                                debt.comment(),
                                debt.amount(),
                                debt.status(),
                                debt.createdAt(),
                                debt.settledAt()
                        )).toList()
                ))
                .toList());
    }

    @PostMapping("/debts/{debtId}/settle")
    public ResponseEntity<Void> settleDebt(@RequestHeader("Authorization") String authorization, @PathVariable("debtId") UUID debtId, @RequestBody(required = false) SettleDebtRequest request) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        service.execute(new SettleDebtCommand(actor.email(), actor.roles(), debtId, request == null ? null : request.comment()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/debts/{debtId}/reattach-to-session")
    public ResponseEntity<Void> reattachDebtToSession(@RequestHeader("Authorization") String authorization, @PathVariable("debtId") UUID debtId, @RequestBody ReattachDebtToSessionRequest request) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        service.execute(new ReattachDebtToSessionCommand(actor.email(), actor.roles(), debtId, request.sessionId()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sales/{saleId}/create-debt")
    public ResponseEntity<Void> createDebtFromSale(@RequestHeader("Authorization") String authorization, @PathVariable("saleId") UUID saleId) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        service.execute(new CreateDebtFromSaleCommand(actor.email(), actor.roles(), saleId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/archive/export")
    public ResponseEntity<byte[]> archiveCustomers(@RequestHeader("Authorization") String authorization, @RequestBody ArchiveCustomersRequest request) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        var result = service.execute(new ArchiveCustomersCommand(actor.email(), actor.roles(), request.startDate(), request.endDate(), request.type(), request.format()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .header("X-Archived-Count", String.valueOf(result.archivedCustomers()))
                .contentType(MediaType.parseMediaType(result.mediaType()))
                .body(result.content());
    }

    @GetMapping("/archive/debts-report")
    public ResponseEntity<byte[]> archiveDebtsReport(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate,
            @RequestParam(name = "type", required = false) String type
    ) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        var result = service.execute(new ExportCustomerDebtReportQuery(actor.email(), startDate, endDate, type));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .contentType(MediaType.parseMediaType(result.mediaType()))
                .body(result.content());
    }
}
