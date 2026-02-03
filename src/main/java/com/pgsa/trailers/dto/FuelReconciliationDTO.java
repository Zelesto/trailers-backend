package com.pgsa.trailers.dto;

import java.math.BigDecimal;

/**
 * FuelReconciliationDTO
 *
 * Represents fuel account reconciliation over a period.
 * Null-safe to avoid NPEs in reporting and analytics.
 */
public record FuelReconciliationDTO(
        String accountName,
        BigDecimal slipsTotal,
        BigDecimal paymentsTotal,
        BigDecimal variance
) {

        public FuelReconciliationDTO(String accountName,
                                     BigDecimal slipsTotal,
                                     BigDecimal paymentsTotal,
                                     BigDecimal variance) {
                this.accountName = accountName != null ? accountName : "";
                this.slipsTotal = slipsTotal != null ? slipsTotal : BigDecimal.ZERO;
                this.paymentsTotal = paymentsTotal != null ? paymentsTotal : BigDecimal.ZERO;
                this.variance = variance != null ? variance : BigDecimal.ZERO;
        }
}
