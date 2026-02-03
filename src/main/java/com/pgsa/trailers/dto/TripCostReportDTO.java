package com.pgsa.trailers.dto;

import java.math.BigDecimal;

/**
 * TripCostReportDTO
 *
 * Represents a detailed cost breakdown of a trip.
 * Null-safe to avoid NPEs during reporting or aggregation.
 */
public record TripCostReportDTO(
        Long tripId,
        String vehicleReg,
        BigDecimal distanceKm,
        BigDecimal fuelCost,
        BigDecimal tollCost,
        BigDecimal foodCost,
        BigDecimal adverseCost,
        BigDecimal totalCost
) {

        public TripCostReportDTO(Long tripId,
                                 String vehicleReg,
                                 BigDecimal distanceKm,
                                 BigDecimal fuelCost,
                                 BigDecimal tollCost,
                                 BigDecimal foodCost,
                                 BigDecimal adverseCost,
                                 BigDecimal totalCost) {
                this.tripId = tripId;
                this.vehicleReg = vehicleReg != null ? vehicleReg : "";
                this.distanceKm = distanceKm != null ? distanceKm : BigDecimal.ZERO;
                this.fuelCost = fuelCost != null ? fuelCost : BigDecimal.ZERO;
                this.tollCost = tollCost != null ? tollCost : BigDecimal.ZERO;
                this.foodCost = foodCost != null ? foodCost : BigDecimal.ZERO;
                this.adverseCost = adverseCost != null ? adverseCost : BigDecimal.ZERO;
                this.totalCost = totalCost != null ? totalCost : BigDecimal.ZERO;
        }
}
