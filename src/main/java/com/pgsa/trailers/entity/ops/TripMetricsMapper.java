package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.TripMetricsDTO;
import org.springframework.stereotype.Component;

@Component
public class TripMetricsMapper {

    public TripMetricsDTO toDto(TripMetrics metrics) {
        if (metrics == null) return null;

        TripMetricsDTO dto = new TripMetricsDTO();
        dto.setTripId(metrics.getTrip().getId());
        dto.setTotalDistanceKm(metrics.getTotalDistanceKm());
        dto.setTotalDurationHours(metrics.getTotalDurationHours());
        dto.setFuelUsedLiters(metrics.getFuelUsedLiters());
        dto.setAverageSpeedKmh(metrics.getAverageSpeedKmh());
        dto.setIdleTimeHours(metrics.getIdleTimeHours());
        dto.setIncidentCount(metrics.getIncidentCount());
        dto.setTasksCompleted(metrics.getTasksCompleted());
        dto.setRevenueAmount(metrics.getRevenueAmount());
        dto.setCostAmount(metrics.getCostAmount());

        return dto;
    }
}
