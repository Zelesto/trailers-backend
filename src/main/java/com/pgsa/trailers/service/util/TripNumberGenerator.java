// src/main/java/com/pgsa/trailers/service/util/TripNumberGenerator.java
package com.pgsa.trailers.service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Component
@RequiredArgsConstructor
@Slf4j
public class TripNumberGenerator {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public String generate() {
        int year = Year.now().getValue();
        String prefix = "TRP-" + year + "-";
        
        try {
            // PostgreSQL version - use ON CONFLICT instead of ON DUPLICATE KEY
            Long nextNumber = jdbcTemplate.queryForObject(
                "INSERT INTO trip_number_sequence (year, next_number) VALUES (?, 1) " +
                "ON CONFLICT (year) DO UPDATE SET next_number = trip_number_sequence.next_number + 1 " +
                "RETURNING next_number - 1",
                new Object[]{String.valueOf(year)},
                Long.class
            );
            
            String tripNumber = prefix + String.format("%06d", nextNumber);
            log.debug("Generated trip number: {}", tripNumber);
            return tripNumber;
            
        } catch (Exception e) {
            log.error("Error generating trip number: {}", e.getMessage());
            // Fallback: use timestamp
            String fallback = prefix + System.currentTimeMillis();
            log.warn("Using fallback trip number: {}", fallback);
            return fallback;
        }
    }
}
