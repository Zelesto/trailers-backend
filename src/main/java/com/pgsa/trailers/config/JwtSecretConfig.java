package com.pgsa.trailers.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

@Configuration
public class JwtSecretConfig {

    /**
     * Inject the secret from application.properties or environment.
     * Example property:
     *   security.jwt.secret=mysuperlongbase64secretstring...
     */
    @Value("${security.jwt.secret}")
    private String secret;

    @Bean
    public Key jwtSigningKey() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured! Please set 'security.jwt.secret'.");
        }

        // If the secret is already base64 encoded, decode it.
        // Otherwise, you can encode it yourself before putting it in properties.
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            // Fallback: treat the secret as a raw string
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
