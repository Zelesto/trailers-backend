package com.pgsa.trailers.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TripAggregateKpiDTO {

    private Long totalTrips;
    private BigDecimal totalDistanceKm;
    private BigDecimal totalFuelUsed;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal totalProfit;

    private BigDecimal avgDistancePerTrip;
    private BigDecimal avgFuelPerTrip;
    private BigDecimal avgProfitPerTrip;
}
