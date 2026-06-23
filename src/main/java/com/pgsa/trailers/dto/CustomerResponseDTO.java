// src/main/java/com/pgsa/trailers/dto/CustomerResponseDTO.java
package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponseDTO {
    private Long id;
    private String customerCode;
    private String name;
    private String registrationNumber;
    private String vatNumber;
    private String email;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String province;
    private String postalCode;
    private String country;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String paymentTerms;
    private BigDecimal creditLimit;
    private Boolean isActive;
    private String notes;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private Integer tripCount;
    private BigDecimal totalSpent;
}
