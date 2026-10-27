package com.pgsa.trailers;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class RenderDeploymentConfigTest {

    @Test
    void renderProductionConfigDisablesDatabaseInitialization() throws IOException {
        String renderConfig = Files.readString(Path.of("render.yaml"));

        assertFalse(renderConfig.contains("key: APP_DB_INIT_ENABLED\n        value: \"true\""),
                "Production deployment must not enable automatic database schema initialization");
        assertFalse(renderConfig.contains("key: APP_DB_INIT_SEED\n        value: \"true\""),
                "Production deployment must not seed the database on startup");
    }

    @Test
    void baseApplicationConfigDoesNotMutateSchemaByDefault() throws IOException {
        String appConfig = Files.readString(Path.of("src/main/resources/application.properties"));

        assertFalse(appConfig.contains("spring.jpa.hibernate.ddl-auto=update"),
                "Base configuration must not enable automatic schema updates in production-safe defaults");
        assertFalse(appConfig.contains("spring.sql.init.mode=always"),
                "Base configuration must not run SQL initialization scripts by default");
    }
}
