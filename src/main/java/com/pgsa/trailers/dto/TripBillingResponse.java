// src/main/java/com/pgsa/trailers/dto/TripBillingResponse.java
package com.pgsa.trailers.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TripBillingResponse {
    private Long id;
    private Long tripId;
    private String tripNumber;
    private Long customerId;
    private String customerName;
    private Long rateId;
    private String rateName;
    private BigDecimal distanceKm;
    private Boolean isEstimate;
    private BigDecimal distanceCharge;
    private BigDecimal tonnageCharge;
    private BigDecimal dailyRateCharge;
    private BigDecimal labourCharge;
    private BigDecimal craneCharge;
    private BigDecimal fixedSurcharge;
    private BigDecimal subtotal;
    private BigDecimal vat;
    private BigDecimal total;
    private String currency;
    private String status;
    private LocalDateTime calculatedAt;
    private Long calculatedBy;
}

