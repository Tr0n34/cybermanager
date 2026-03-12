package com.cybermanager.infrastructure.adapters.persistence.catalog;

import com.cybermanager.domain.model.catalog.Product;
import com.cybermanager.domain.model.catalog.ProductId;
import com.cybermanager.domain.model.catalog.ProductStatus;
import com.cybermanager.domain.port.catalog.ProductRepository;
import com.cybermanager.infrastructure.entities.persistence.catalog.ProductJpaEntity;
import com.cybermanager.infrastructure.repositories.persistence.catalog.ProductJpaRepository;
import com.cybermanager.domain.model.shared.Money;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProductRepositoryAdapter implements ProductRepository {
    private final ProductJpaRepository repository;

    public ProductRepositoryAdapter(ProductJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Product save(Product product) {
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.id = product.id().value();
        entity.name = product.name();
        entity.price = product.price().amount();
        entity.category = product.category();
        entity.status = product.status().name();
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Product> findById(ProductId productId) {
        return repository.findById(productId.value()).map(this::toDomain);
    }

    @Override
    public void deleteById(ProductId productId) {
        repository.deleteById(productId.value());
    }

    @Override
    public List<Product> search(String term, ProductStatus status, String category) {
        String search = term == null ? "" : term.toLowerCase();
        return repository.findAll().stream()
                .filter(item -> search.isBlank() || item.name.toLowerCase().contains(search))
                .filter(item -> status == null || item.status.equals(status.name()))
                .filter(item -> category == null || category.isBlank() || item.category.equalsIgnoreCase(category))
                .map(this::toDomain)
                .toList();
    }

    private Product toDomain(ProductJpaEntity entity) {
        return new Product(new ProductId(entity.id), entity.name, new Money(entity.price), entity.category, ProductStatus.valueOf(entity.status));
    }
}

