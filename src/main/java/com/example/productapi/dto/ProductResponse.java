package com.example.productapi.dto;

import com.example.productapi.model.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(Long id, String name, String description, BigDecimal price,
                              String category, Integer stockQuantity, LocalDateTime createdAt) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.id(), product.name(), product.description(), product.price(),
                product.category(), product.stockQuantity(), product.createdAt());
    }
}
