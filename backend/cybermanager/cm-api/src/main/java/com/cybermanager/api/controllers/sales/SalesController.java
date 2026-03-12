package com.cybermanager.api.controllers.sales;

import com.cybermanager.api.dtos.sales.SalesDtos.*;
import com.cybermanager.application.commands.sales.ConfigureConnectionPricingCommand;
import com.cybermanager.application.commands.sales.CreateConnectionTimeSaleCommand;
import com.cybermanager.application.commands.sales.CreateProductSaleCommand;
import com.cybermanager.application.commands.sales.CreateSubscriptionSaleCommand;
import com.cybermanager.application.queries.sales.GetSaleDetailsQuery;
import com.cybermanager.application.queries.sales.SearchSalesOfDayQuery;
import com.cybermanager.application.services.sales.SalesApplicationService;
import com.cybermanager.application.views.sales.SaleView;
import com.cybermanager.api.shared.ApiSupport;
import com.cybermanager.infrastructure.security.users.JwtAccessTokenReader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class SalesController {
    private final SalesApplicationService service;
    private final JwtAccessTokenReader tokenReader;

    public SalesController(SalesApplicationService service, JwtAccessTokenReader tokenReader) {
        this.service = service;
        this.tokenReader = tokenReader;
    }

    @GetMapping("/api/sales/day")
    public ResponseEntity<List<SaleResponse>> day(@RequestParam(name = "date", required = false) String date) {
        LocalDate parsed = date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date);
        return ResponseEntity.ok(service.execute(new SearchSalesOfDayQuery(parsed)).stream().map(this::toResponse).toList());
    }

    @GetMapping("/api/sales/{id}")
    public ResponseEntity<SaleResponse> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(toResponse(service.execute(new GetSaleDetailsQuery(id))));
    }

    @PostMapping("/api/sales/products")
    public ResponseEntity<SaleResponse> productSale(@RequestHeader("Authorization") String authorization, @RequestBody ProductSaleRequest request) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        var view = service.execute(new CreateProductSaleCommand(actor.email(), actor.roles(), request.customerId(), request.lines().stream().map(line -> new CreateProductSaleCommand.ProductSaleLineCommand(line.productId(), line.quantity())).toList(), request.createDebt()));
        return ResponseEntity.ok(toResponse(view));
    }

    @PostMapping("/api/sales/subscriptions")
    public ResponseEntity<SaleResponse> subscriptionSale(@RequestHeader("Authorization") String authorization, @RequestBody SubscriptionSaleRequest request) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        return ResponseEntity.ok(toResponse(service.execute(new CreateSubscriptionSaleCommand(actor.email(), actor.roles(), request.customerId(), request.subscriptionOfferId(), request.createDebt()))));
    }

    @PostMapping("/api/sales/connection-time")
    public ResponseEntity<SaleResponse> connectionSale(@RequestHeader("Authorization") String authorization, @RequestBody ConnectionTimeSaleRequest request) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        return ResponseEntity.ok(toResponse(service.execute(new CreateConnectionTimeSaleCommand(actor.email(), actor.roles(), request.customerId(), request.minutes(), request.createDebt()))));
    }

    @PutMapping("/api/pricing/connection-time")
    public ResponseEntity<PricingResponse> configurePricing(@RequestHeader("Authorization") String authorization, @RequestBody PricingRequest request) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        var view = service.execute(new ConfigureConnectionPricingCommand(
                actor.email(),
                actor.roles(),
                request.tiers().stream()
                        .map(tier -> new ConfigureConnectionPricingCommand.PricingTierCommand(tier.hours(), tier.minutes(), tier.price()))
                        .toList()
        ));
        return ResponseEntity.ok(toPricingResponse(view));
    }

    @GetMapping("/api/pricing/connection-time")
    public ResponseEntity<PricingResponse> getPricing() {
        var view = service.getCurrentPricing();
        return ResponseEntity.ok(toPricingResponse(view));
    }

    private SaleResponse toResponse(SaleView view) {
        return new SaleResponse(view.saleId(), view.customerId(), view.type(), view.soldAt(), view.lines().stream().map(line -> new SaleLineResponse(line.label(), line.quantity(), line.unitPrice(), line.totalPrice())).toList(), view.totalAmount());
    }

    private PricingResponse toPricingResponse(com.cybermanager.application.views.sales.ConnectionPricingView view) {
        return new PricingResponse(view.tiers().stream()
                .map(tier -> new PricingTierResponse(tier.hours(), tier.minutes(), tier.durationMinutes(), tier.price()))
                .toList());
    }
}

