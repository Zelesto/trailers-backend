package com.pgsa.trailers.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Configuration
public class JwtSecretConfig {

    private final String secret;

    /**
     * Injects the JWT secret from environment or application properties.
     * Example property:
     *   security.jwt.secret=mysuperlongbase64secretstring...
     *
     * @param secret the JWT secret key, base64 encoded preferred
     */
    public JwtSecretConfig(@Value("${security.jwt.secret:dev-secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT secret is not configured! Please set 'security.jwt.secret' in your environment or properties."
            );
        }
        this.secret = secret;
    }

    /**
     * Returns the signing key for JWT generation/validation.
     * Tries to decode the secret as Base64; falls back to UTF-8 bytes if decoding fails.
     */
    @Bean
    public Key jwtSigningKey() {
        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < 32) {
            try {
                keyBytes = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException ex) {
                throw new IllegalStateException("Unable to generate a secure JWT key", ex);
            }
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}
