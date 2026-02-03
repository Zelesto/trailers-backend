package com.pgsa.trailers.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TripMetricsResponse {

    private BigDecimal totalDistanceKm;
    private BigDecimal totalDurationHours;
    private BigDecimal idleTimeHours;
    private BigDecimal averageSpeedKmh;

    private BigDecimal fuelUsedLiters;

    private Integer incidentCount;
    private Integer tasksCompleted;

    private BigDecimal revenueAmount;
    private BigDecimal costAmount;
}
