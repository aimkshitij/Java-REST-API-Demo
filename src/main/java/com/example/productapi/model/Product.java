package com.example.productapi.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Product(Long id, String name, String description, BigDecimal price,
                      String category, Integer stockQuantity, LocalDateTime createdAt) {
}
