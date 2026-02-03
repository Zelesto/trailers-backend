package com.pgsa.trailers.dto;

import java.math.BigDecimal;

/**
 * FuelFraudDTO
 *
 * Simple DTO for fuel variance reporting.
 * Null-safe constructor for consistent Analytics usage.
 */
public record FuelFraudDTO(
        String registration,
        BigDecimal expectedLiters,
        BigDecimal actualLiters,
        BigDecimal variance
) {

        public FuelFraudDTO(String registration, BigDecimal expectedLiters, BigDecimal actualLiters, BigDecimal variance) {
                this.registration = registration;
                this.expectedLiters = expectedLiters != null ? expectedLiters : BigDecimal.ZERO;
                this.actualLiters = actualLiters != null ? actualLiters : BigDecimal.ZERO;
                this.variance = variance != null ? variance : BigDecimal.ZERO;
        }
}
