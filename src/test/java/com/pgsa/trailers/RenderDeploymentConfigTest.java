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
}
