package com.shantanu.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class OrderSnapshotBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderSnapshotBackfill.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public OrderSnapshotBackfill(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql")) {
                return;
            }
        }

        int updatedRows = jdbcTemplate.update("""
                UPDATE order_item oi
                SET item_name = COALESCE(oi.item_name, f.name),
                    unit_price = COALESCE(
                        oi.unit_price,
                        CASE
                            WHEN oi.quantity > 0 AND oi.total_price IS NOT NULL
                                THEN oi.total_price / oi.quantity
                            ELSE f.price
                        END
                    )
                FROM food f
                WHERE oi.food_id = f.id
                  AND (oi.item_name IS NULL OR oi.unit_price IS NULL)
                """);

        if (updatedRows > 0) {
            log.info("Backfilled immutable snapshots for {} existing order items", updatedRows);
        }
    }
}
