package com.shantanu.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConnectionConfigTest {

    @Test
    void convertsNeonConnectionStringToJdbcDetails() {
        DatabaseConnectionConfig.ParsedDatabaseUrl result =
                DatabaseConnectionConfig.parseDatabaseUrl(
                        "postgresql://dinehub_user:p%40ss%2Bword@ep-example.neon.tech/neondb"
                                + "?sslmode=require&channel_binding=require"
                );

        assertThat(result.jdbcUrl()).isEqualTo(
                "jdbc:postgresql://ep-example.neon.tech/neondb"
                        + "?sslmode=require&channelBinding=require"
        );
        assertThat(result.username()).isEqualTo("dinehub_user");
        assertThat(result.password()).isEqualTo("p@ss+word");
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
