// src/main/java/com/pgsa/trailers/config/SupabaseS3Config.java - Simple Version
package com.pgsa.trailers.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@Slf4j
public class SupabaseS3Config {

    private final Environment environment;

    @Value("${supabase.s3.endpoint}")
    private String endpoint;

    @Value("${supabase.s3.region:eu-central-1}")
    private String region;

    @Value("${supabase.s3.access-key-id}")
    private String accessKeyId;

    @Value("${supabase.s3.secret-access-key}")
    private String secretAccessKey;

    @Value("${supabase.s3.bucket}")
    private String bucketName;

    @Value("${supabase.url}")
    private String supabaseUrl;

    public SupabaseS3Config(Environment environment) {
        this.environment = environment;
    }

    private boolean isStorageConfigured() {
        return StringUtils.hasText(endpoint)
                && StringUtils.hasText(accessKeyId)
                && StringUtils.hasText(secretAccessKey)
                && StringUtils.hasText(bucketName)
                && StringUtils.hasText(supabaseUrl);
    }

    @Bean
    public S3Client supabaseS3Client() {
        String[] activeProfiles = environment.getActiveProfiles();
        String profile = activeProfiles.length > 0 ? activeProfiles[0] : "default";

        log.info("========================================");
        log.info("Supabase S3 Configuration");
        log.info("Active Profile: {}", profile);
        log.info("Endpoint: {}", endpoint);
        log.info("Region: {}", region);
        log.info("Bucket: {}", bucketName);
        log.info("Supabase URL: {}", supabaseUrl);
        log.info("========================================");

        if (!isStorageConfigured()) {
            log.warn("Supabase storage is not configured; skipping S3 client initialization for local/test boot.");
            return S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("local-dev-access-key", "local-dev-secret-key")
                    ))
                    .build();
        }

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    @Bean
    public S3Presigner supabaseS3Presigner() {
        if (!isStorageConfigured()) {
            log.warn("Supabase storage is not configured; skipping S3 presigner initialization for local/test boot.");
            return S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("local-dev-access-key", "local-dev-secret-key")
                    ))
                    .build();
        }

        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }
}
