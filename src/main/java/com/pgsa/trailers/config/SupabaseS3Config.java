// src/main/java/com/pgsa/trailers/config/SupabaseS3Config.java
package com.pgsa.trailers.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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

        // Validate credentials are not empty
        if (accessKeyId == null || accessKeyId.isEmpty()) {
            log.error("Supabase Access Key ID is missing! Check your configuration.");
            throw new IllegalStateException("Supabase Access Key ID is required");
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
