package com.example.productapi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "Name is required") String name,
        @Size(max = 1000, message = "Description must be at most 1000 characters") String description,
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0", inclusive = false, message = "Price must be greater than zero") BigDecimal price,
        String category,
        @NotNull(message = "Stock quantity is required")
        @Min(value = 0, message = "Stock quantity must be nonnegative") Integer stockQuantity) {
}
