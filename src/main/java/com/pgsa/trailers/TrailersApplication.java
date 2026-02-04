package com.pgsa.trailers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TrailersApplication implements CommandLineRunner {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${security.jwt.secret:NOT_SET}")
    private String jwtSecret;

    public static void main(String[] args) {
        SpringApplication.run(TrailersApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== Active Spring Profile: " + activeProfile + " ===");

        if ("NOT_SET".equals(jwtSecret)) {
            System.err.println("!!! WARNING: JWT_SECRET is not set in environment variables !!!");
        } else {
            System.out.println("JWT secret is loaded successfully.");
        }
    }
}
