// src/main/java/com/pgsa/trailers/dto/BillingChargeDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingChargeDTO {
    private Long id;
    private String chargeType;
    private String description;
    private BigDecimal amount;
    private String notes;
}
