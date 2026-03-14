package com.cybermanager.api.controllers.catalog;

import com.cybermanager.api.dtos.catalog.ProductDtos.ProductRequest;
import com.cybermanager.api.dtos.catalog.ProductDtos.ProductResponse;
import com.cybermanager.application.commands.catalog.ActivateProductCommand;
import com.cybermanager.application.commands.catalog.CreateProductCommand;
import com.cybermanager.application.commands.catalog.DeactivateProductCommand;
import com.cybermanager.application.commands.catalog.DeleteProductCommand;
import com.cybermanager.application.commands.catalog.UpdateProductCommand;
import com.cybermanager.application.queries.catalog.GetProductDetailsQuery;
import com.cybermanager.application.queries.catalog.SearchProductsQuery;
import com.cybermanager.application.services.catalog.ProductApplicationService;
import com.cybermanager.api.shared.ApiSupport;
import com.cybermanager.infrastructure.security.users.JwtAccessTokenReader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductApplicationService service;
    private final JwtAccessTokenReader tokenReader;

    public ProductController(ProductApplicationService service, JwtAccessTokenReader tokenReader) {
        this.service = service;
        this.tokenReader = tokenReader;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> search(
            @RequestParam(name = "term", required = false) String term,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "category", required = false) String category
    ) {
        return ResponseEntity.ok(service.execute(new SearchProductsQuery(term, status, category)).stream().map(view -> new ProductResponse(view.productId(), view.name(), view.price(), view.category(), view.description(), view.status())).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> get(@PathVariable("id") UUID id) {
        var view = service.execute(new GetProductDetailsQuery(id));
        return ResponseEntity.ok(new ProductResponse(view.productId(), view.name(), view.price(), view.category(), view.description(), view.status()));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@RequestHeader("Authorization") String authorization, @RequestBody ProductRequest request) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        var view = service.execute(new CreateProductCommand(actor.email(), actor.roles(), request.name(), request.price(), request.category(), request.description()));
        return ResponseEntity.ok(new ProductResponse(view.productId(), view.name(), view.price(), view.category(), view.description(), view.status()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@RequestHeader("Authorization") String authorization, @PathVariable("id") UUID id, @RequestBody ProductRequest request) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        var view = service.execute(new UpdateProductCommand(actor.email(), actor.roles(), id, request.name(), request.price(), request.category(), request.description()));
        return ResponseEntity.ok(new ProductResponse(view.productId(), view.name(), view.price(), view.category(), view.description(), view.status()));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ProductResponse> activate(@RequestHeader("Authorization") String authorization, @PathVariable("id") UUID id) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        var view = service.execute(new ActivateProductCommand(actor.email(), actor.roles(), id));
        return ResponseEntity.ok(new ProductResponse(view.productId(), view.name(), view.price(), view.category(), view.description(), view.status()));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ProductResponse> deactivate(@RequestHeader("Authorization") String authorization, @PathVariable("id") UUID id) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        var view = service.execute(new DeactivateProductCommand(actor.email(), actor.roles(), id));
        return ResponseEntity.ok(new ProductResponse(view.productId(), view.name(), view.price(), view.category(), view.description(), view.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("Authorization") String authorization, @PathVariable("id") UUID id) {
        var actor = ApiSupport.actor(authorization, tokenReader);
        service.execute(new DeleteProductCommand(actor.email(), actor.roles(), id));
        return ResponseEntity.noContent().build();
    }
}

