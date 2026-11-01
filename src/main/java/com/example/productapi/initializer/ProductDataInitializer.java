package com.example.productapi.initializer;

import com.example.productapi.dto.ProductRequest;
import com.example.productapi.service.ProductService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductDataInitializer implements CommandLineRunner {
    private final ProductService service;

    public ProductDataInitializer(ProductService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        service.create(new ProductRequest("Wireless Mouse", "Ergonomic wireless mouse with adjustable DPI",
                new BigDecimal("29.99"), "Accessories", 50));
        service.create(new ProductRequest("Mechanical Keyboard", "Backlit mechanical keyboard with tactile switches",
                new BigDecimal("89.99"), "Accessories", 25));
        service.create(new ProductRequest("USB-C Hub", "Seven-port USB-C hub with HDMI and power delivery",
                new BigDecimal("49.99"), "Connectivity", 40));
        service.create(new ProductRequest("Monitor Stand", "Adjustable aluminum monitor stand with desk storage",
                new BigDecimal("39.99"), "Office", 30));
        service.create(new ProductRequest("Laptop Backpack", "Water-resistant backpack for laptops up to 16 inches",
                new BigDecimal("59.99"), "Bags", 20));
    }
}
