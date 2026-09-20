package com.shantanu.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PostgresSequenceSynchronizer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PostgresSequenceSynchronizer.class);

    private static final Map<String, String> ENTITY_SEQUENCES = new LinkedHashMap<>();

    static {
        ENTITY_SEQUENCES.put("users_seq", "users");
        ENTITY_SEQUENCES.put("restaurant_seq", "restaurant");
        ENTITY_SEQUENCES.put("category_seq", "category");
        ENTITY_SEQUENCES.put("food_seq", "food");
        ENTITY_SEQUENCES.put("ingredient_category_seq", "ingredient_category");
        ENTITY_SEQUENCES.put("ingredients_item_seq", "ingredients_item");
        ENTITY_SEQUENCES.put("cart_seq", "cart");
        ENTITY_SEQUENCES.put("cart_item_seq", "cart_item");
    }

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public PostgresSequenceSynchronizer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
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

        ENTITY_SEQUENCES.forEach(this::advanceSequenceWhenStale);
    }

    private void advanceSequenceWhenStale(String sequenceName, String tableName) {
        String existingSequence = jdbcTemplate.queryForObject(
                "SELECT to_regclass(?)::text",
                String.class,
                sequenceName
        );
        if (existingSequence == null) return;

        Long maxId = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(id), 0) FROM " + tableName,
                Long.class
        );
        Long sequenceValue = jdbcTemplate.queryForObject(
                "SELECT last_value FROM " + sequenceName,
                Long.class
        );

        if (maxId != null && sequenceValue != null && maxId >= sequenceValue && maxId > 0) {
            jdbcTemplate.queryForObject(
                    "SELECT setval(CAST(? AS regclass), ?, true)",
                    Long.class,
                    sequenceName,
                    maxId
            );
            log.info("Advanced stale database sequence {} beyond existing {} identifiers", sequenceName, tableName);
        }
    }
}
