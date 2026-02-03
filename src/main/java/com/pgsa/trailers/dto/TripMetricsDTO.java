package com.pgsa.trailers.dto;

import com.pgsa.trailers.entity.ops.TripMetrics;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class TripMetricsDTO {

    // Identity
    private Long tripId;

    // Core metrics
    private BigDecimal totalDistanceKm;
    private BigDecimal totalDurationHours;
    private BigDecimal fuelUsedLiters;
    private BigDecimal idleTimeHours;

    // Derived metrics
    private BigDecimal averageSpeedKmh;
    private BigDecimal fuelEfficiencyKmPerLiter;

    // Operational metrics
    private Integer incidentCount;
    private Integer tasksCompleted;

    // Financials
    private BigDecimal revenueAmount;
    private BigDecimal costAmount;

    // Context (not persisted on metrics)
    private String originLocation;
    private String destinationLocation;
    private String vehicleType;

    public static TripMetricsDTO fromEntity(TripMetrics entity) {
        if (entity == null) {
            return null;
        }

        TripMetricsDTO dto = new TripMetricsDTO();

        // Identity
        if (entity.getTrip() != null) {
            dto.setTripId(entity.getTrip().getId());
            dto.setOriginLocation(entity.getTrip().getOriginLocation());
            dto.setDestinationLocation(entity.getTrip().getDestinationLocation());
            dto.setVehicleType(
                    entity.getTrip().getVehicle() != null
                            ? entity.getTrip().getVehicle().getVehicleType().name()
                            : null
            );
        }

        // Core metrics
        dto.setTotalDistanceKm(entity.getTotalDistanceKm());
        dto.setTotalDurationHours(entity.getTotalDurationHours());
        dto.setFuelUsedLiters(entity.getFuelUsedLiters());
        dto.setIdleTimeHours(entity.getIdleTimeHours());

        // Derived metrics
        dto.setAverageSpeedKmh(entity.getAverageSpeedKmh());
        dto.setFuelEfficiencyKmPerLiter(calculateFuelEfficiency(
                entity.getTotalDistanceKm(),
                entity.getFuelUsedLiters()
        ));

        // Operational
        dto.setIncidentCount(entity.getIncidentCount());
        dto.setTasksCompleted(entity.getTasksCompleted());

        // Financials
        dto.setRevenueAmount(entity.getRevenueAmount());
        dto.setCostAmount(entity.getCostAmount());

        return dto;
    }

    private static BigDecimal calculateFuelEfficiency(
            BigDecimal distanceKm,
            BigDecimal fuelLiters
    ) {
        if (distanceKm == null ||
                fuelLiters == null ||
                fuelLiters.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return distanceKm.divide(fuelLiters, 2, RoundingMode.HALF_UP);
    }
}
