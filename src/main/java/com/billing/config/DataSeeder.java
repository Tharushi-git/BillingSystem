package com.billing.config;

import com.billing.model.Product;
import com.billing.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Adds a few sample products the first time the app starts, so you can try it straight away. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository products;

    public DataSeeder(ProductRepository products) {
        this.products = products;
    }

    @Override
    public void run(String... args) {
        if (products.count() == 0) {
            products.save(new Product("Rice 1kg", new BigDecimal("250.00"), 50));
            products.save(new Product("Milk 1L", new BigDecimal("180.00"), 30));
            products.save(new Product("Bread", new BigDecimal("120.00"), 20));
        }
    }
}
