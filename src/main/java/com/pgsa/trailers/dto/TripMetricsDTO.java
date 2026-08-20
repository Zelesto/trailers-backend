package com.pgsa.trailers.dto;

import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.ops.TripMetrics;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripMetricsDTO {
    private Long id;
    private Long tripId;
    private String tripNumber;
    private String vehicleType;
    private String originLocation;
    private String destinationLocation;
    
    // Distance & time
    private BigDecimal totalDistanceKm;
    private BigDecimal totalDurationHours;
    private BigDecimal idleTimeHours;
    private BigDecimal averageSpeedKmh;
    
    // Fuel
    private BigDecimal fuelUsedLiters;
    private BigDecimal fuelEfficiencyKmPerLiter;
    
    // Activity
    private Integer incidentCount;
    private Integer tasksCompleted;
    
    // Financial
    private BigDecimal revenueAmount;
    private BigDecimal costAmount;
    
    // Location-based
    private BigDecimal originCityTravelTimeHours;
    private BigDecimal destinationCityTravelTimeHours;
    private BigDecimal plannedVsActualDistanceVarianceKm;
    private BigDecimal plannedVsActualDurationVarianceHours;
    private BigDecimal geocodingConfidenceScore;
    
    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean finalized;
    private LocalDateTime finalizedAt;

    // ====== EXPLICIT GETTERS AND SETTERS ======

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }

    public String getTripNumber() { return tripNumber; }
    public void setTripNumber(String tripNumber) { this.tripNumber = tripNumber; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getOriginLocation() { return originLocation; }
    public void setOriginLocation(String originLocation) { this.originLocation = originLocation; }

    public String getDestinationLocation() { return destinationLocation; }
    public void setDestinationLocation(String destinationLocation) { this.destinationLocation = destinationLocation; }

    public BigDecimal getTotalDistanceKm() { return totalDistanceKm; }
    public void setTotalDistanceKm(BigDecimal totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }

    public BigDecimal getTotalDurationHours() { return totalDurationHours; }
    public void setTotalDurationHours(BigDecimal totalDurationHours) { this.totalDurationHours = totalDurationHours; }

    public BigDecimal getIdleTimeHours() { return idleTimeHours; }
    public void setIdleTimeHours(BigDecimal idleTimeHours) { this.idleTimeHours = idleTimeHours; }

    public BigDecimal getAverageSpeedKmh() { return averageSpeedKmh; }
    public void setAverageSpeedKmh(BigDecimal averageSpeedKmh) { this.averageSpeedKmh = averageSpeedKmh; }

    public BigDecimal getFuelUsedLiters() { return fuelUsedLiters; }
    public void setFuelUsedLiters(BigDecimal fuelUsedLiters) { this.fuelUsedLiters = fuelUsedLiters; }

    public BigDecimal getFuelEfficiencyKmPerLiter() { return fuelEfficiencyKmPerLiter; }
    public void setFuelEfficiencyKmPerLiter(BigDecimal fuelEfficiencyKmPerLiter) { 
        this.fuelEfficiencyKmPerLiter = fuelEfficiencyKmPerLiter; 
    }

    public Integer getIncidentCount() { return incidentCount; }
    public void setIncidentCount(Integer incidentCount) { this.incidentCount = incidentCount; }

    public Integer getTasksCompleted() { return tasksCompleted; }
    public void setTasksCompleted(Integer tasksCompleted) { this.tasksCompleted = tasksCompleted; }

    public BigDecimal getRevenueAmount() { return revenueAmount; }
    public void setRevenueAmount(BigDecimal revenueAmount) { this.revenueAmount = revenueAmount; }

    public BigDecimal getCostAmount() { return costAmount; }
    public void setCostAmount(BigDecimal costAmount) { this.costAmount = costAmount; }

    public BigDecimal getOriginCityTravelTimeHours() { return originCityTravelTimeHours; }
    public void setOriginCityTravelTimeHours(BigDecimal originCityTravelTimeHours) { 
        this.originCityTravelTimeHours = originCityTravelTimeHours; 
    }

    public BigDecimal getDestinationCityTravelTimeHours() { return destinationCityTravelTimeHours; }
    public void setDestinationCityTravelTimeHours(BigDecimal destinationCityTravelTimeHours) { 
        this.destinationCityTravelTimeHours = destinationCityTravelTimeHours; 
    }

    public BigDecimal getPlannedVsActualDistanceVarianceKm() { return plannedVsActualDistanceVarianceKm; }
    public void setPlannedVsActualDistanceVarianceKm(BigDecimal plannedVsActualDistanceVarianceKm) { 
        this.plannedVsActualDistanceVarianceKm = plannedVsActualDistanceVarianceKm; 
    }

    public BigDecimal getPlannedVsActualDurationVarianceHours() { return plannedVsActualDurationVarianceHours; }
    public void setPlannedVsActualDurationVarianceHours(BigDecimal plannedVsActualDurationVarianceHours) { 
        this.plannedVsActualDurationVarianceHours = plannedVsActualDurationVarianceHours; 
    }

    public BigDecimal getGeocodingConfidenceScore() { return geocodingConfidenceScore; }
    public void setGeocodingConfidenceScore(BigDecimal geocodingConfidenceScore) { 
        this.geocodingConfidenceScore = geocodingConfidenceScore; 
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getFinalized() { return finalized; }
    public void setFinalized(Boolean finalized) { this.finalized = finalized; }

    public LocalDateTime getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(LocalDateTime finalizedAt) { this.finalizedAt = finalizedAt; }

    // ====== STATIC FACTORY METHODS ======

    /**
     * Convert TripMetrics entity to DTO
     */
    public static TripMetricsDTO fromEntity(TripMetrics entity) {
        if (entity == null) return null;
        
        TripMetricsDTO dto = new TripMetricsDTO();
        dto.setId(entity.getId());
        
        Trip trip = entity.getTrip();
        if (trip != null) {
            dto.setTripId(trip.getId());
            dto.setTripNumber(trip.getTripNumber());
            dto.setOriginLocation(trip.getOriginLocation());
            dto.setDestinationLocation(trip.getDestinationLocation());
            // FIXED: Removed .name() call - vehicleType is String
            if (trip.getVehicle() != null && trip.getVehicle().getVehicleType() != null) {
                dto.setVehicleType(trip.getVehicle().getVehicleType());
            }
        }
        
        dto.setTotalDistanceKm(entity.getTotalDistanceKm());
        dto.setTotalDurationHours(entity.getTotalDurationHours());
        dto.setIdleTimeHours(entity.getIdleTimeHours());
        dto.setAverageSpeedKmh(entity.getAverageSpeedKmh());
        dto.setFuelUsedLiters(entity.getFuelUsedLiters());
        dto.setIncidentCount(entity.getIncidentCount());
        dto.setTasksCompleted(entity.getTasksCompleted());
        dto.setRevenueAmount(entity.getRevenueAmount());
        dto.setCostAmount(entity.getCostAmount());
        dto.setOriginCityTravelTimeHours(entity.getOriginCityTravelTimeHours());
        dto.setDestinationCityTravelTimeHours(entity.getDestinationCityTravelTimeHours());
        dto.setPlannedVsActualDistanceVarianceKm(entity.getPlannedVsActualDistanceVarianceKm());
        dto.setPlannedVsActualDurationVarianceHours(entity.getPlannedVsActualDurationVarianceHours());
        dto.setGeocodingConfidenceScore(entity.getGeocodingConfidenceScore());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setFinalized(entity.isFinalized());
        dto.setFinalizedAt(entity.getFinalizedAt());
        
        // Calculate fuel efficiency
        if (dto.getTotalDistanceKm() != null && dto.getFuelUsedLiters() != null 
                && dto.getFuelUsedLiters().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal efficiency = dto.getTotalDistanceKm().divide(dto.getFuelUsedLiters(), 2, RoundingMode.HALF_UP);
            dto.setFuelEfficiencyKmPerLiter(efficiency);
        } else {
            dto.setFuelEfficiencyKmPerLiter(BigDecimal.ZERO);
        }
        
        return dto;
    }

    /**
     * Convert DTO to entity
     */
    public TripMetrics toEntity() {
        TripMetrics entity = new TripMetrics();
        entity.setId(this.id);
        entity.setTotalDistanceKm(this.totalDistanceKm);
        entity.setTotalDurationHours(this.totalDurationHours);
        entity.setIdleTimeHours(this.idleTimeHours);
        entity.setAverageSpeedKmh(this.averageSpeedKmh);
        entity.setFuelUsedLiters(this.fuelUsedLiters);
        entity.setIncidentCount(this.incidentCount);
        entity.setTasksCompleted(this.tasksCompleted);
        entity.setRevenueAmount(this.revenueAmount);
        entity.setCostAmount(this.costAmount);
        entity.setOriginCityTravelTimeHours(this.originCityTravelTimeHours);
        entity.setDestinationCityTravelTimeHours(this.destinationCityTravelTimeHours);
        entity.setPlannedVsActualDistanceVarianceKm(this.plannedVsActualDistanceVarianceKm);
        entity.setPlannedVsActualDurationVarianceHours(this.plannedVsActualDurationVarianceHours);
        entity.setGeocodingConfidenceScore(this.geocodingConfidenceScore);
        entity.setFinalized(this.finalized != null ? this.finalized : false);
        entity.setFinalizedAt(this.finalizedAt);
        return entity;
    }
}
