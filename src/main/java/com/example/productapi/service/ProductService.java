package com.example.productapi.service;

import com.example.productapi.dto.ProductRequest;
import com.example.productapi.exception.ProductNotFoundException;
import com.example.productapi.model.Product;
import com.example.productapi.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public Product create(ProductRequest request) {
        return repository.create(new Product(null, request.name(), request.description(), request.price(),
                request.category(), request.stockQuantity(), LocalDateTime.now()));
    }

    public List<Product> findAll() {
        return repository.findAll();
    }

    public Product findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Product update(Long id, ProductRequest request) {
        Product existing = findById(id);
        int stockQuantity = existing.stockQuantity();
        if (request.stockQuantity() > 0) {
            stockQuantity = request.stockQuantity();
        }
        Product updated = new Product(id, request.name(), request.description(), request.price(),
                request.category(), stockQuantity, existing.createdAt());
        return repository.replaceIfPresent(updated).orElseThrow(() -> new ProductNotFoundException(id));
    }

    public void delete(Long id) {
        if (!repository.deleteById(id)) {
            throw new ProductNotFoundException(id);
        }
    }
}
