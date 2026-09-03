// src/main/java/com/pgsa/trailers/dto/LoadBillingResponse.java
package com.pgsa.trailers.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;



@Data
public class LoadBillingResponse {
    private Long id;
    private String loadId;
    private String loadNumber;
    private Long customerId;
    private String customerName;
    private Integer tripCount;
    private Integer completedTrips;
    private BigDecimal subtotal;
    private BigDecimal vat;
    private BigDecimal total;
    private String currency;
    private String status;
    private List<TripBillingResponse> tripBillings;
}
