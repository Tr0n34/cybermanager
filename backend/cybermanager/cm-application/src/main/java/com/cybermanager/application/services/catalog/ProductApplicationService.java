package com.cybermanager.application.services.catalog;

import com.cybermanager.application.commands.catalog.ActivateProductCommand;
import com.cybermanager.application.commands.catalog.CreateProductCommand;
import com.cybermanager.application.commands.catalog.DeactivateProductCommand;
import com.cybermanager.application.commands.catalog.DeleteProductCommand;
import com.cybermanager.application.commands.catalog.UpdateProductCommand;
import com.cybermanager.application.queries.catalog.GetProductDetailsQuery;
import com.cybermanager.application.queries.catalog.SearchProductsQuery;
import com.cybermanager.domain.model.catalog.Product;
import com.cybermanager.domain.model.catalog.ProductId;
import com.cybermanager.domain.model.catalog.ProductStatus;
import com.cybermanager.domain.port.catalog.ProductRepository;
import com.cybermanager.application.services.shared.ActorSupport;
import com.cybermanager.application.usecases.catalog.ActivateProductUseCase;
import com.cybermanager.application.usecases.catalog.CreateProductUseCase;
import com.cybermanager.application.usecases.catalog.DeactivateProductUseCase;
import com.cybermanager.application.usecases.catalog.DeleteProductUseCase;
import com.cybermanager.application.usecases.catalog.GetProductDetailsUseCase;
import com.cybermanager.application.usecases.catalog.SearchProductsUseCase;
import com.cybermanager.application.usecases.catalog.UpdateProductUseCase;
import com.cybermanager.application.views.catalog.ProductView;
import com.cybermanager.domain.model.shared.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductApplicationService implements
        CreateProductUseCase,
        UpdateProductUseCase,
        ActivateProductUseCase,
        DeactivateProductUseCase,
        DeleteProductUseCase,
        SearchProductsUseCase,
        GetProductDetailsUseCase {
    private final ProductRepository productRepository;

    public ProductApplicationService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductView execute(CreateProductCommand command) {
        ActorSupport.requireAdmin(command.actorRoles());
        return toView(productRepository.save(Product.create(command.name(), new Money(command.price()), command.category(), command.description())));
    }

    @Override
    public ProductView execute(UpdateProductCommand command) {
        ActorSupport.requireAdmin(command.actorRoles());
        Product product = productRepository.findById(new ProductId(command.productId()))
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        return toView(productRepository.save(product.update(command.name(), new Money(command.price()), command.category(), command.description())));
    }

    @Override
    public ProductView execute(ActivateProductCommand command) {
        ActorSupport.requireAdmin(command.actorRoles());
        Product product = productRepository.findById(new ProductId(command.productId()))
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        return toView(productRepository.save(product.activate()));
    }

    @Override
    public ProductView execute(DeactivateProductCommand command) {
        ActorSupport.requireAdmin(command.actorRoles());
        Product product = productRepository.findById(new ProductId(command.productId()))
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        return toView(productRepository.save(product.deactivate()));
    }

    @Override
    public void execute(DeleteProductCommand command) {
        ActorSupport.requireAdmin(command.actorRoles());
        ProductId productId = new ProductId(command.productId());
        productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        productRepository.deleteById(productId);
    }

    @Transactional(readOnly = true)
    public List<ProductView> execute(SearchProductsQuery query) {
        ProductStatus status = query.status() == null || query.status().isBlank() ? null : ProductStatus.valueOf(query.status().toUpperCase());
        return productRepository.search(query.term(), status, query.category()).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public ProductView execute(GetProductDetailsQuery query) {
        return productRepository.findById(new ProductId(query.productId()))
                .map(this::toView)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    private ProductView toView(Product product) {
        return new ProductView(product.id().value(), product.name(), product.price().amount(), product.category(), product.description(), product.status().name());
    }
}

