package com.example.productapi.repository;

import com.example.productapi.model.Product;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryProductRepository implements ProductRepository {
    private final ConcurrentMap<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public Product create(Product product) {
        long id = nextId.getAndIncrement();
        Product saved = new Product(id, product.name(), product.description(), product.price(),
                product.category(), product.stockQuantity(), product.createdAt());
        products.put(id, saved);
        return saved;
    }

    @Override
    public List<Product> findAll() {
        return products.values().stream().sorted(Comparator.comparing(Product::id)).toList();
    }

    @Override
    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public Optional<Product> replaceIfPresent(Product product) {
        return Optional.ofNullable(products.computeIfPresent(product.id(), (id, existing) -> product));
    }

    @Override
    public boolean deleteById(Long id) {
        return products.remove(id) != null;
    }
}
