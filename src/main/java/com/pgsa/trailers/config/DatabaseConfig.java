package com.pgsa.trailers.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@Profile("production")
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() throws URISyntaxException {
        String databaseUrl = System.getenv("DATABASE_URL");

        if (databaseUrl == null) {
            // Use default for local development
            return DataSourceBuilder.create()
                    .url("jdbc:postgresql://localhost:5432/logistics_db")
                    .username("postgres")
                    .password("password")
                    .build();
        }

        // Parse DATABASE_URL (postgresql://user:pass@host:port/dbname)
        if (databaseUrl.startsWith("postgresql://")) {
            databaseUrl = databaseUrl.replace("postgresql://", "jdbc:postgresql://");
        }

        // Add SSL if not present
        if (!databaseUrl.contains("?")) {
            databaseUrl += "?sslmode=require";
        } else if (!databaseUrl.contains("sslmode=")) {
            databaseUrl += "&sslmode=require";
        }

        return DataSourceBuilder.create()
                .url(databaseUrl)
                .build();
    }
}