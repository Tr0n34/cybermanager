package com.cybermanager.api.controllers.subscription;

import com.cybermanager.api.shared.ApiSupport;
import com.cybermanager.api.dtos.subscription.SubscriptionOfferDtos.SubscriptionOfferRequest;
import com.cybermanager.api.dtos.subscription.SubscriptionOfferDtos.SubscriptionOfferResponse;
import com.cybermanager.application.commands.subscription.ActivateSubscriptionOfferCommand;
import com.cybermanager.application.commands.subscription.CreateSubscriptionOfferCommand;
import com.cybermanager.application.commands.subscription.DeactivateSubscriptionOfferCommand;
import com.cybermanager.application.commands.subscription.DeleteSubscriptionOfferCommand;
import com.cybermanager.application.commands.subscription.UpdateSubscriptionOfferCommand;
import com.cybermanager.application.queries.subscription.GetSubscriptionOfferDetailsQuery;
import com.cybermanager.application.queries.subscription.SearchSubscriptionOffersQuery;
import com.cybermanager.application.services.subscription.SubscriptionApplicationService;
import com.cybermanager.infrastructure.security.users.JwtAccessTokenReader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscription-offers")
public class SubscriptionOfferController {
    private final SubscriptionApplicationService service;
    private final JwtAccessTokenReader tokenReader;

    public SubscriptionOfferController(SubscriptionApplicationService service, JwtAccessTokenReader tokenReader) {
        this.service = service;
        this.tokenReader = tokenReader;
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionOfferResponse>> search(
            @RequestParam(name = "term", required = false) String term,
            @RequestParam(name = "status", required = false) String status
    ) {
        return ResponseEntity.ok(service.execute(new SearchSubscriptionOffersQuery(term, status)).stream().map(view -> new SubscriptionOfferResponse(view.offerId(), view.name(), view.price(), view.includedMinutes(), view.status())).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionOfferResponse> get(@PathVariable("id") UUID id) {
        var view = service.execute(new GetSubscriptionOfferDetailsQuery(id));
        return ResponseEntity.ok(new SubscriptionOfferResponse(view.offerId(), view.name(), view.price(), view.includedMinutes(), view.status()));
    }

    @PostMapping
    public ResponseEntity<SubscriptionOfferResponse> create(@RequestHeader("Authorization") String authorization, @RequestBody SubscriptionOfferRequest request) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        var view = service.execute(new CreateSubscriptionOfferCommand(actor.email(), actor.roles(), request.name(), request.price(), request.includedMinutes()));
        return ResponseEntity.ok(new SubscriptionOfferResponse(view.offerId(), view.name(), view.price(), view.includedMinutes(), view.status()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionOfferResponse> update(@RequestHeader("Authorization") String authorization, @PathVariable("id") UUID id, @RequestBody SubscriptionOfferRequest request) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        var view = service.execute(new UpdateSubscriptionOfferCommand(actor.email(), actor.roles(), id, request.name(), request.price(), request.includedMinutes()));
        return ResponseEntity.ok(new SubscriptionOfferResponse(view.offerId(), view.name(), view.price(), view.includedMinutes(), view.status()));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<SubscriptionOfferResponse> activate(@RequestHeader("Authorization") String authorization, @PathVariable("id") UUID id) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        var view = service.execute(new ActivateSubscriptionOfferCommand(actor.email(), actor.roles(), id));
        return ResponseEntity.ok(new SubscriptionOfferResponse(view.offerId(), view.name(), view.price(), view.includedMinutes(), view.status()));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<SubscriptionOfferResponse> deactivate(@RequestHeader("Authorization") String authorization, @PathVariable("id") UUID id) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        var view = service.execute(new DeactivateSubscriptionOfferCommand(actor.email(), actor.roles(), id));
        return ResponseEntity.ok(new SubscriptionOfferResponse(view.offerId(), view.name(), view.price(), view.includedMinutes(), view.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("Authorization") String authorization, @PathVariable("id") UUID id) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        service.execute(new DeleteSubscriptionOfferCommand(actor.email(), actor.roles(), id));
        return ResponseEntity.noContent().build();
    }
}

