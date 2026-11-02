package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DirtiesContext
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.flyway.target=3")
class ApiMigrationLoadTest {
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", DATABASE::getJdbcUrl);
        properties.add("spring.datasource.username", DATABASE::getUsername);
        properties.add("spring.datasource.password", DATABASE::getPassword);
    }

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private DataSource dataSource;

    @Test
    void servesOrdersWhileContractMigrationRuns() throws Exception {
        Map<?, ?> product = rest.postForObject("/products", Map.of("name", "Book", "price", 10), Map.class);
        long productId = ((Number) product.get("id")).longValue();
        var errors = new CopyOnWriteArrayList<Throwable>();
        var running = new AtomicBoolean(true);
        var contracted = new AtomicBoolean();
        var requests = new AtomicInteger();
        var started = new CountDownLatch(4);
        var finished = new CountDownLatch(4);
        var pool = Executors.newFixedThreadPool(4);
        try {
            for (int i = 0; i < 4; i++) {
                pool.submit(() -> {
                    while (running.get()) {
                        try {
                            exerciseApi(productId);
                            requests.incrementAndGet();
                            started.countDown();
                            if (contracted.get()) {
                                finished.countDown();
                            }
                        } catch (Throwable error) {
                            errors.add(error);
                            running.set(false);
                        }
                    }
                });
            }
            assertThat(started.await(30, TimeUnit.SECONDS)).as("requests before migration").isTrue();
            Flyway.configure().dataSource(dataSource).load().migrate();
            contracted.set(true);
            assertThat(finished.await(30, TimeUnit.SECONDS)).as("requests after migration").isTrue();
        } finally {
            running.set(false);
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(errors).isEmpty();
        assertThat(requests.get()).isGreaterThanOrEqualTo(8);
        assertThat(new JdbcTemplate(dataSource).queryForObject(
                "SELECT count(*) FROM customers WHERE first_name = 'Load' AND last_name = 'Customer'",
                Integer.class)).isEqualTo(requests.get());
    }

    private void exerciseApi(long productId) {
        var request = new CreateOrderRequest("Load Customer", "Street", null, "regular", null,
                List.of(new CreateOrderRequest.Item(productId, 1)));
        var created = rest.postForEntity("/orders", request, Map.class);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        var listed = rest.getForEntity("/orders", List.class);
        assertThat(listed.getStatusCode().value()).isEqualTo(200);
        assertThat(listed.getBody()).isNotEmpty();
        assertThat((Map<?, ?>) listed.getBody().get(0))
                .extracting(order -> order.get("customerFullName")).isEqualTo("Load Customer");
    }
}
