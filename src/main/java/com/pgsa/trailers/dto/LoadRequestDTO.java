// src/main/java/com/pgsa/trailers/dto/LoadRequestDTO.java
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
public class LoadRequestDTO {
    private String loadNumber;
    private String loadType;
    private String description;
    private Long customerId;
    private String status;
    private String priority;
    private BigDecimal estimatedValue;
    private BigDecimal actualValue;
    private String notes;
}
