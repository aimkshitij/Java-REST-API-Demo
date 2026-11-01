package com.example.productapi.repository;

import com.example.productapi.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product create(Product product);
    List<Product> findAll();
    Optional<Product> findById(Long id);
    Optional<Product> replaceIfPresent(Product product);
    boolean deleteById(Long id);
}
