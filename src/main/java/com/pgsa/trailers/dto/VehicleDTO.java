package com.pgsa.trailers.dto;

import lombok.Data;

import java.time.LocalDate;
import java.math.BigDecimal;

@Data
public class VehicleDTO {
    private Long id;
    private String registration_number;
    private String vin;
    private String make;
    private String model;
    private Integer year;
    private String fuelType;
    private BigDecimal currentMileage;
    private String status;
    private BigDecimal avgConsumption;
    private BigDecimal currentOdometer;
    private LocalDate lastServiceDate;
    private Integer serviceIntervalDays;
    private BigDecimal serviceIntervalKm;
}
