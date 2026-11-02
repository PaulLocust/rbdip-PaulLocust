package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderValidator {
    private final ProductRepository products;

    public OrderValidator(ProductRepository products) {
        this.products = products;
    }

    public List<ValidatedItem> validate(CreateOrderRequest request) {
        requireText(request.customerFullName(), "customerFullName is required");
        requireText(request.customerAddress(), "customerAddress is required");
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }
        return request.items().stream().map(this::validateItem).toList();
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private ValidatedItem validateItem(CreateOrderRequest.Item item) {
        Product product = products.findById(item.productId())
                .orElseThrow(() -> new IllegalArgumentException("product " + item.productId() + " not found"));
        int quantity = item.quantity() == null ? 1 : item.quantity();
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return new ValidatedItem(product, quantity);
    }

    public record ValidatedItem(Product product, int quantity) {
        public PricingCalculator.LineItem priceLine() {
            return new PricingCalculator.LineItem(product.getPrice(), quantity);
        }
    }
}
