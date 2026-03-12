package com.cybermanager.api.controllers.customer;

import com.cybermanager.api.dtos.customer.CustomerDtos.*;
import com.cybermanager.api.shared.ApiSupport;
import com.cybermanager.application.commands.customer.ConvertCustomerToSubscriberCommand;
import com.cybermanager.application.commands.customer.CreateDebtFromSaleCommand;
import com.cybermanager.application.commands.customer.CreateCustomerCommand;
import com.cybermanager.application.commands.customer.SettleDebtCommand;
import com.cybermanager.application.commands.customer.UpdateCustomerCommand;
import com.cybermanager.application.queries.customer.GetCustomerDetailsQuery;
import com.cybermanager.application.queries.customer.SearchOpenDebtsQuery;
import com.cybermanager.application.queries.customer.SearchCustomersQuery;
import com.cybermanager.application.services.customer.CustomerApplicationService;
import com.cybermanager.infrastructure.security.users.JwtAccessTokenReader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
                        purchase.type(),
                        purchase.label(),
                        purchase.soldAt(),
                        purchase.totalAmount(),
                        purchase.openDebt()
                )).toList(),
                view.debts().stream().map(debt -> new CustomerDebtResponse(
                        debt.debtId(),
                        debt.label(),
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
        var view = service.execute(new ConvertCustomerToSubscriberCommand(actor.email(), actor.roles(), id, request.subscriptionOfferId(), request.deductCurrentSession()));
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
                                debt.amount(),
                                debt.status(),
                                debt.createdAt(),
                                debt.settledAt()
                        )).toList()
                ))
                .toList());
    }

    @PostMapping("/debts/{debtId}/settle")
    public ResponseEntity<Void> settleDebt(@RequestHeader("Authorization") String authorization, @PathVariable("debtId") UUID debtId) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        service.execute(new SettleDebtCommand(actor.email(), actor.roles(), debtId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sales/{saleId}/create-debt")
    public ResponseEntity<Void> createDebtFromSale(@RequestHeader("Authorization") String authorization, @PathVariable("saleId") UUID saleId) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        service.execute(new CreateDebtFromSaleCommand(actor.email(), actor.roles(), saleId));
        return ResponseEntity.noContent().build();
    }
}
