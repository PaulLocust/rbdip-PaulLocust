package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class NormalizationMigrationTest {
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void preservesLegacyOrdersAndReusesMatchingCatalogEntries() {
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setUrl(DATABASE.getJdbcUrl());
        source.setUser(DATABASE.getUsername());
        source.setPassword(DATABASE.getPassword());
        Flyway.configure().dataSource(source).target("1").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.update("INSERT INTO products (name, price) VALUES ('Book', 10), ('Book', 10)");
        jdbc.update("INSERT INTO orders (customer_full_name, customer_address)"
                + " VALUES ('Anna Smith', 'Street'), ('Anna Smith', 'Street'), ('Anna Smith', 'Other')");
        jdbc.update("INSERT INTO order_items (order_id, product_name, product_price, quantity)"
                + " VALUES (1, 'Book', 10, 2), (2, 'Book', 9, 3), (3, 'Missing book', 20, 1)");

        Flyway.configure().dataSource(source).target("2").load().migrate();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM customers", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM products", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT product_id FROM order_items WHERE id = 1", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT p.price * i.quantity FROM order_items i"
                + " JOIN products p ON p.id = i.product_id ORDER BY i.id", java.math.BigDecimal.class))
                .containsExactly(new java.math.BigDecimal("20.00"), new java.math.BigDecimal("27.00"),
                        new java.math.BigDecimal("20.00"));
        assertThat(jdbc.queryForList("SELECT customer_id FROM orders ORDER BY id", Long.class))
                .satisfies(ids -> {
                    assertThat(ids.get(0)).isEqualTo(ids.get(1));
                    assertThat(ids.get(2)).isNotEqualTo(ids.get(0));
                });
    }
}
