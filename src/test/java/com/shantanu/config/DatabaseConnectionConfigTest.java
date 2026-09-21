package com.shantanu.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConnectionConfigTest {

    @Test
    void convertsPostgresConnectionStringToJdbcUrl() {
        DatabaseConnectionConfig.ParsedDatabaseUrl result =
                DatabaseConnectionConfig.parseDatabaseUrl(
                        "postgresql://database.example.invalid/testdb"
                                + "?sslmode=require&channel_binding=require"
                );

        assertThat(result.jdbcUrl()).isEqualTo(
                "jdbc:postgresql://database.example.invalid/testdb"
                        + "?sslmode=require&channelBinding=require"
        );
        assertThat(result.username()).isNull();
        assertThat(result.password()).isNull();
    }

    @Test
    void leavesExistingJdbcUrlUnchanged() {
        DatabaseConnectionConfig.ParsedDatabaseUrl result =
                DatabaseConnectionConfig.parseDatabaseUrl(
                        "jdbc:postgresql://localhost:5432/food_ordering_app"
                );

        assertThat(result.jdbcUrl())
                .isEqualTo("jdbc:postgresql://localhost:5432/food_ordering_app");
        assertThat(result.username()).isNull();
        assertThat(result.password()).isNull();
    }
}
