package com.shantanu.config;

import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
public class DatabaseConnectionConfig {

    @Bean
    JdbcConnectionDetails jdbcConnectionDetails(Environment environment) {
        String configuredUrl = firstNonBlank(
                environment.getProperty("spring.datasource.url"),
                environment.getProperty("DB_URL"),
                environment.getProperty("DATABASE_URL")
        );

        if (configuredUrl == null) {
            throw new IllegalStateException(
                    "Database URL is missing. Configure DB_URL or DATABASE_URL."
            );
        }

        ParsedDatabaseUrl parsedUrl = parseDatabaseUrl(configuredUrl);
        String username = firstNonBlank(
                environment.getProperty("DB_USERNAME"),
                parsedUrl.username(),
                environment.getProperty("spring.datasource.username")
        );
        String password = firstNonBlank(
                environment.getProperty("DB_PASSWORD"),
                parsedUrl.password(),
                environment.getProperty("spring.datasource.password")
        );

        if (parsedUrl.jdbcUrl().startsWith("jdbc:postgresql:")
                && (username == null || password == null)) {
            throw new IllegalStateException(
                    "PostgreSQL credentials are missing. Supply them inside DATABASE_URL "
                            + "or configure DB_USERNAME and DB_PASSWORD."
            );
        }

        return new JdbcConnectionDetails() {
            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public String getPassword() {
                return password == null ? "" : password;
            }

            @Override
            public String getJdbcUrl() {
                return parsedUrl.jdbcUrl();
            }
        };
    }

    static ParsedDatabaseUrl parseDatabaseUrl(String configuredUrl) {
        String url = configuredUrl.trim();
        if (url.startsWith("jdbc:")) {
            return new ParsedDatabaseUrl(url, null, null);
        }

        if (!url.startsWith("postgres://") && !url.startsWith("postgresql://")) {
            throw new IllegalStateException(
                    "Unsupported database URL. Use a JDBC URL or a postgres:// connection string."
            );
        }

        try {
            URI uri = URI.create(url);
            if (uri.getHost() == null || uri.getRawPath() == null || uri.getRawPath().length() < 2) {
                throw new IllegalArgumentException("host or database name is missing");
            }

            String[] credentials = uri.getRawUserInfo() == null
                    ? new String[0]
                    : uri.getRawUserInfo().split(":", 2);
            String username = credentials.length > 0 ? decodeUriComponent(credentials[0]) : null;
            String password = credentials.length > 1 ? decodeUriComponent(credentials[1]) : null;
            String port = uri.getPort() < 0 ? "" : ":" + uri.getPort();
            String query = normalizePostgresQuery(uri.getRawQuery());
            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + port + uri.getRawPath()
                    + (query.isBlank() ? "" : "?" + query);

            return new ParsedDatabaseUrl(jdbcUrl, username, password);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "The configured PostgreSQL connection string is invalid: " + exception.getMessage(),
                    exception
            );
        }
    }

    private static String normalizePostgresQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }

        return Arrays.stream(rawQuery.split("&"))
                .map(parameter -> parameter.startsWith("channel_binding=")
                        ? "channelBinding=" + parameter.substring("channel_binding=".length())
                        : parameter)
                .collect(Collectors.joining("&"));
    }

    private static String decodeUriComponent(String value) {
        // In URI user-info a plus sign is literal, whereas URLDecoder normally
        // treats it as a space.
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String... values) {
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    record ParsedDatabaseUrl(String jdbcUrl, String username, String password) {
    }
}
