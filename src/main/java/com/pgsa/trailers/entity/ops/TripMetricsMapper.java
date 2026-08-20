package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.TripMetricsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Slf4j
public class TripMetricsMapper {

    // ====== EXPLICIT LOGGER (since @Slf4j may not work) ======
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TripMetricsMapper.class);

    /**
     * Convert TripMetrics entity to TripMetricsDTO
     */
    public TripMetricsDTO toDto(TripMetrics metrics) {
        if (metrics == null) {
            log.debug("⚠️ Cannot convert null metrics to DTO");
            return null;
        }

        TripMetricsDTO dto = new TripMetricsDTO();
        
        // Safely get trip ID
        Trip trip = metrics.getTrip();
        if (trip != null) {
            dto.setTripId(trip.getId());
            dto.setTripNumber(trip.getTripNumber());
            dto.setOriginLocation(trip.getOriginLocation());
            dto.setDestinationLocation(trip.getDestinationLocation());
            // FIXED: Removed .name() call - vehicleType is String
            if (trip.getVehicle() != null && trip.getVehicle().getVehicleType() != null) {
                dto.setVehicleType(trip.getVehicle().getVehicleType());
            }
        } else {
            log.warn("⚠️ TripMetrics with ID {} has no associated Trip", metrics.getId());
        }

        // Set all metrics
        dto.setTotalDistanceKm(metrics.getTotalDistanceKm());
        dto.setTotalDurationHours(metrics.getTotalDurationHours());
        dto.setFuelUsedLiters(metrics.getFuelUsedLiters());
        dto.setAverageSpeedKmh(metrics.getAverageSpeedKmh());
        dto.setIdleTimeHours(metrics.getIdleTimeHours());
        dto.setIncidentCount(metrics.getIncidentCount());
        dto.setTasksCompleted(metrics.getTasksCompleted());
        dto.setRevenueAmount(metrics.getRevenueAmount());
        dto.setCostAmount(metrics.getCostAmount());
        
        // Location-based metrics
        dto.setOriginCityTravelTimeHours(metrics.getOriginCityTravelTimeHours());
        dto.setDestinationCityTravelTimeHours(metrics.getDestinationCityTravelTimeHours());
        dto.setPlannedVsActualDistanceVarianceKm(metrics.getPlannedVsActualDistanceVarianceKm());
        dto.setPlannedVsActualDurationVarianceHours(metrics.getPlannedVsActualDurationVarianceHours());
        dto.setGeocodingConfidenceScore(metrics.getGeocodingConfidenceScore());

        // Audit fields - use getFinalized() for Boolean type
        dto.setCreatedAt(metrics.getCreatedAt());
        dto.setUpdatedAt(metrics.getUpdatedAt());
        dto.setFinalized(metrics.isFinalized());  // metrics.isFinalized() is correct (entity uses primitive boolean)
        dto.setFinalizedAt(metrics.getFinalizedAt());

        // Calculate fuel efficiency
        if (dto.getTotalDistanceKm() != null && dto.getFuelUsedLiters() != null 
                && dto.getFuelUsedLiters().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal efficiency = dto.getTotalDistanceKm().divide(dto.getFuelUsedLiters(), 2, BigDecimal.ROUND_HALF_UP);
            dto.setFuelEfficiencyKmPerLiter(efficiency);
        } else {
            dto.setFuelEfficiencyKmPerLiter(BigDecimal.ZERO);
        }

        log.debug("✅ Converted metrics to DTO for trip ID: {}", dto.getTripId());
        return dto;
    }

    /**
     * Convert TripMetricsDTO to TripMetrics entity
     */
    public TripMetrics toEntity(TripMetricsDTO dto, Trip trip) {
        if (dto == null) {
            log.debug("⚠️ Cannot convert null DTO to entity");
            return null;
        }

        TripMetrics metrics = new TripMetrics();
        
        // Set the trip relationship
        if (trip != null) {
            metrics.setTrip(trip);
        } else {
            log.warn("⚠️ Creating metrics without associated Trip");
        }
        
        // Set all metrics
        metrics.setTotalDistanceKm(dto.getTotalDistanceKm());
        metrics.setTotalDurationHours(dto.getTotalDurationHours());
        metrics.setFuelUsedLiters(dto.getFuelUsedLiters());
        metrics.setAverageSpeedKmh(dto.getAverageSpeedKmh());
        metrics.setIdleTimeHours(dto.getIdleTimeHours());
        metrics.setIncidentCount(dto.getIncidentCount());
        metrics.setTasksCompleted(dto.getTasksCompleted());
        metrics.setRevenueAmount(dto.getRevenueAmount());
        metrics.setCostAmount(dto.getCostAmount());
        
        // Location-based metrics
        metrics.setOriginCityTravelTimeHours(dto.getOriginCityTravelTimeHours());
        metrics.setDestinationCityTravelTimeHours(dto.getDestinationCityTravelTimeHours());
        metrics.setPlannedVsActualDistanceVarianceKm(dto.getPlannedVsActualDistanceVarianceKm());
        metrics.setPlannedVsActualDurationVarianceHours(dto.getPlannedVsActualDurationVarianceHours());
        metrics.setGeocodingConfidenceScore(dto.getGeocodingConfidenceScore());

        // Audit fields - DTO uses Boolean, so use getFinalized()
        Boolean finalized = dto.getFinalized();  // ✅ Use getFinalized() not isFinalized()
        metrics.setFinalized(finalized != null ? finalized : false);
        metrics.setFinalizedAt(dto.getFinalizedAt());

        log.debug("✅ Converted DTO to entity");
        return metrics;
    }

    /**
     * Update existing TripMetrics entity with DTO values
     */
    public void updateEntity(TripMetrics metrics, TripMetricsDTO dto) {
        if (metrics == null) {
            log.warn("⚠️ Cannot update null metrics entity");
            return;
        }
        
        if (dto == null) {
            log.warn("⚠️ Cannot update metrics with null DTO");
            return;
        }

        // Update all metrics
        if (dto.getTotalDistanceKm() != null) {
            metrics.setTotalDistanceKm(dto.getTotalDistanceKm());
        }
        if (dto.getTotalDurationHours() != null) {
            metrics.setTotalDurationHours(dto.getTotalDurationHours());
        }
        if (dto.getFuelUsedLiters() != null) {
            metrics.setFuelUsedLiters(dto.getFuelUsedLiters());
        }
        if (dto.getAverageSpeedKmh() != null) {
            metrics.setAverageSpeedKmh(dto.getAverageSpeedKmh());
        }
        if (dto.getIdleTimeHours() != null) {
            metrics.setIdleTimeHours(dto.getIdleTimeHours());
        }
        if (dto.getIncidentCount() != null) {
            metrics.setIncidentCount(dto.getIncidentCount());
        }
        if (dto.getTasksCompleted() != null) {
            metrics.setTasksCompleted(dto.getTasksCompleted());
        }
        if (dto.getRevenueAmount() != null) {
            metrics.setRevenueAmount(dto.getRevenueAmount());
        }
        if (dto.getCostAmount() != null) {
            metrics.setCostAmount(dto.getCostAmount());
        }
        
        // Location-based metrics
        if (dto.getOriginCityTravelTimeHours() != null) {
            metrics.setOriginCityTravelTimeHours(dto.getOriginCityTravelTimeHours());
        }
        if (dto.getDestinationCityTravelTimeHours() != null) {
            metrics.setDestinationCityTravelTimeHours(dto.getDestinationCityTravelTimeHours());
        }
        if (dto.getPlannedVsActualDistanceVarianceKm() != null) {
            metrics.setPlannedVsActualDistanceVarianceKm(dto.getPlannedVsActualDistanceVarianceKm());
        }
        if (dto.getPlannedVsActualDurationVarianceHours() != null) {
            metrics.setPlannedVsActualDurationVarianceHours(dto.getPlannedVsActualDurationVarianceHours());
        }
        if (dto.getGeocodingConfidenceScore() != null) {
            metrics.setGeocodingConfidenceScore(dto.getGeocodingConfidenceScore());
        }

        // Audit fields - DTO uses Boolean, so use getFinalized()
        Boolean finalized = dto.getFinalized();  // ✅ Use getFinalized() not isFinalized()
        if (finalized != null) {
            metrics.setFinalized(finalized);
        }
        if (dto.getFinalizedAt() != null) {
            metrics.setFinalizedAt(dto.getFinalizedAt());
        }

        log.debug("✅ Updated metrics entity with ID: {}", metrics.getId());
    }

    /**
     * Create a new TripMetrics entity with default values
     */
    public TripMetrics createDefaultMetrics(Trip trip) {
        TripMetrics metrics = new TripMetrics();
        
        if (trip != null) {
            metrics.setTrip(trip);
        }
        
        metrics.setTotalDistanceKm(BigDecimal.ZERO);
        metrics.setTotalDurationHours(BigDecimal.ZERO);
        metrics.setIdleTimeHours(BigDecimal.ZERO);
        metrics.setAverageSpeedKmh(BigDecimal.ZERO);
        metrics.setFuelUsedLiters(BigDecimal.ZERO);
        metrics.setIncidentCount(0);
        metrics.setTasksCompleted(0);
        metrics.setRevenueAmount(BigDecimal.ZERO);
        metrics.setCostAmount(BigDecimal.ZERO);
        metrics.setOriginCityTravelTimeHours(BigDecimal.ZERO);
        metrics.setDestinationCityTravelTimeHours(BigDecimal.ZERO);
        metrics.setPlannedVsActualDistanceVarianceKm(BigDecimal.ZERO);
        metrics.setPlannedVsActualDurationVarianceHours(BigDecimal.ZERO);
        metrics.setGeocodingConfidenceScore(BigDecimal.ZERO);
        metrics.setFinalized(false);
        
        log.debug("✅ Created default metrics for trip ID: {}", trip != null ? trip.getId() : "null");
        return metrics;
    }
}
