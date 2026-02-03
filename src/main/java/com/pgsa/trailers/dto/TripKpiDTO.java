package com.pgsa.trailers.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TripKpiDTO(
        Long tripId,
        String tripNumber,
        String status,
        LocalDate plannedStartDate,
        BigDecimal totalDistanceKm,
        BigDecimal fuelUsed,
        BigDecimal revenueAmount,
        BigDecimal costAmount,
        BigDecimal profit,
        BigDecimal profitMargin
) {}

