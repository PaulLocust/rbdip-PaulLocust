package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CustomerNameMigrationTest {
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void backfillsNamesAndSynchronizesBothApplicationVersions() {
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setUrl(DATABASE.getJdbcUrl());
        source.setUser(DATABASE.getUsername());
        source.setPassword(DATABASE.getPassword());
        Flyway.configure().dataSource(source).target("2").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(source);
        List<String> names = List.of("Anna Maria Smith", "Prince", "Anna ", " Anna", "Anna  Smith");
        names.forEach(name -> jdbc.update("INSERT INTO customers (full_name) VALUES (?)", name));

        Flyway.configure().dataSource(source).target("3").load().migrate();
        assertThat(jdbc.queryForList("SELECT first_name || CASE WHEN last_name = '' THEN ''"
                + " ELSE ' ' || last_name END FROM customers ORDER BY id", String.class)).isEqualTo(names);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customers"
                + " WHERE first_name IS NULL OR last_name IS NULL", Integer.class)).isZero();

        jdbc.update("INSERT INTO customers (full_name) VALUES ('Old Writer')");
        jdbc.update("INSERT INTO customers (first_name, last_name) VALUES ('New', 'Writer')");
        assertThat(jdbc.queryForObject("SELECT first_name FROM customers WHERE full_name = 'Old Writer'",
                String.class)).isEqualTo("Old");
        assertThat(jdbc.queryForObject("SELECT full_name FROM customers WHERE first_name = 'New'",
                String.class)).isEqualTo("New Writer");
        jdbc.update("UPDATE customers SET full_name = 'Legacy Updated' WHERE first_name = 'Old'");
        jdbc.update("UPDATE customers SET last_name = 'Updated' WHERE first_name = 'New'");
        assertThat(jdbc.queryForObject("SELECT last_name FROM customers WHERE first_name = 'Legacy'",
                String.class)).isEqualTo("Updated");
        assertThat(jdbc.queryForObject("SELECT full_name FROM customers WHERE first_name = 'New'",
                String.class)).isEqualTo("New Updated");

        Flyway.configure().dataSource(source).load().migrate();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customers", Integer.class)).isEqualTo(7);
        assertThat(jdbc.queryForList("SELECT column_name FROM information_schema.columns"
                + " WHERE table_name = 'customers'", String.class)).doesNotContain("full_name");
    }
}
