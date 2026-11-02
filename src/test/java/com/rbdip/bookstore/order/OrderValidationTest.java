package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbdip.bookstore.reference.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OrderValidationTest extends AbstractIntegrationTest {
    @ParameterizedTest
    @CsvSource({", Street, customerFullName is required", "' ', Street, customerFullName is required",
        "Anna, , customerAddress is required", "Anna, ' ', customerAddress is required"})
    void rejectsMissingContactInformation(String name, String address, String error) {
        assertRejected(new CreateOrderRequest(name, address, null, null, null, List.of()), error);
    }

    @Test
    void rejectsMissingItemsAndUnknownProductsWithoutPartialOrders() {
        assertRejected(request(null), "order must contain at least one item");
        assertRejected(request(List.of()), "order must contain at least one item");
        long productId = product();
        assertRejected(request(List.of(new CreateOrderRequest.Item(productId, 1),
                new CreateOrderRequest.Item(999999L, 1))), "product 999999 not found");
    }

    @Test
    void rejectsNonPositiveQuantityAndDefaultsMissingQuantityToOne() {
        long productId = product();
        assertRejected(request(List.of(new CreateOrderRequest.Item(productId, 0))), "quantity must be positive");
        assertRejected(request(List.of(new CreateOrderRequest.Item(productId, -1))), "quantity must be positive");
        var response = restTemplate.postForEntity("/orders",
                request(List.of(new CreateOrderRequest.Item(productId, null))), Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(jdbcTemplate.queryForObject("SELECT quantity FROM order_items", Integer.class)).isEqualTo(1);
    }

    @Test
    void productAndReviewEndpointsPreserveTheirResponses() {
        long productId = product();
        var products = restTemplate.getForEntity("/products", List.class);
        assertThat(products.getBody()).hasSize(1);
        assertThat(((Map<?, ?>) products.getBody().get(0)).get("price")).isEqualTo("10.00");
        var review = restTemplate.postForEntity("/products/" + productId + "/reviews",
                Map.of("rating", 5), Map.class);
        assertThat(review.getStatusCode().value()).isEqualTo(201);
        var reviews = restTemplate.getForEntity("/products/" + productId + "/reviews", List.class);
        assertThat(reviews.getBody()).containsExactly(Map.of("authorName", "anonymous", "rating", 5, "comment", ""));
    }

    private void assertRejected(CreateOrderRequest request, String message) {
        var response = restTemplate.postForEntity("/orders", request, Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", message);
        for (String table : List.of("customers", "orders", "order_items")) {
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Integer.class)).isZero();
        }
    }

    private long product() {
        Map<?, ?> result = restTemplate.postForObject("/products",
                Map.of("name", "Book", "price", 10, "description", "Test book"), Map.class);
        return ((Number) result.get("id")).longValue();
    }

    private CreateOrderRequest request(List<CreateOrderRequest.Item> items) {
        return new CreateOrderRequest("Anna Smith", "Street", null, null, null, items);
    }
}
