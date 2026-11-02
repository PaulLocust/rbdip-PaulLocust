package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbdip.bookstore.purchase.PurchaseHistory;
import com.rbdip.bookstore.reference.AbstractIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderQueriesTest extends AbstractIntegrationTest {
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private PurchaseHistory purchases;

    @Test
    void loadsAllItemsWithoutGrowingQueryCount() {
        long first = product("First");
        long second = product("Second");
        order("Anna Smith", first, second);
        assertListing(1);
        for (int i = 0; i < 19; i++) {
            order("Buyer " + i, first, second);
        }
        assertListing(20);
    }

    @Test
    void purchaseHistoryMatchesBothBuyerAndProduct() {
        long bought = product("Bought");
        long other = product("Other");
        order("Anna Smith", bought, bought);
        assertThat(purchases.hasPurchased(bought, "Anna Smith")).isTrue();
        assertThat(purchases.hasPurchased(other, "Anna Smith")).isFalse();
        assertThat(purchases.hasPurchased(bought, "Someone Else")).isFalse();
        assertThat(purchases.hasPurchased(bought, null)).isFalse();
        order("Prince", other, other);
        assertThat(purchases.hasPurchased(other, "Prince")).isTrue();
    }

    private void assertListing(int count) {
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var response = restTemplate.getForEntity("/orders", List.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(count);
        for (Object raw : response.getBody()) {
            var items = (List<?>) ((Map<?, ?>) raw).get("items");
            assertThat(items).hasSize(2);
            assertThat(items.stream().map(item -> (String) ((Map<?, ?>) item).get("productName")).toList())
                    .containsExactlyInAnyOrder("First", "Second");
        }
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    private long product(String name) {
        Map<?, ?> body = restTemplate.postForObject("/products", Map.of("name", name, "price", 10), Map.class);
        return ((Number) body.get("id")).longValue();
    }

    private void order(String buyer, long first, long second) {
        var request = new CreateOrderRequest(buyer, "Street", null, null, null, List.of(
                new CreateOrderRequest.Item(first, 1), new CreateOrderRequest.Item(second, 2)));
        assertThat(restTemplate.postForEntity("/orders", request, Map.class).getStatusCode().value())
                .isEqualTo(201);
    }
}
