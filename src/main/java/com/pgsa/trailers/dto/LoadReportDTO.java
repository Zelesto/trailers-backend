// src/main/java/com/pgsa/trailers/dto/report/LoadReportDTO.java

package com.pgsa.trailers.dto.report;

import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadReportDTO {
    private String loadNumber;
    private String status;
    private String description;
    private String commodityType;
    private BigDecimal weightKg;
    private Integer palletCount;
    private Integer tripCount;
    private String customerName;
    
    private BigDecimal totalDistanceKm;
    private BigDecimal totalFromDepotKm;
    private BigDecimal totalToDepotKm;
    private BigDecimal totalDepotKm;
    
    private String createdAt;
    private String updatedAt;
    private List<TripSummary> trips;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripSummary {
        private Long tripId;
        private String tripNumber;
        private String driverName;
        private String vehicleRegistration;
        private String plannedStartDate;
        private String plannedEndDate;
        private BigDecimal actualDistanceKm;
        private String status;
    }

    public static LoadReportDTO fromEntity(Load load, List<Trip> trips) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

        List<TripSummary> tripSummaries = trips.stream()
                .map(trip -> {
                    String driverName = null;
                    if (trip.getDriver() != null) {
                        String firstName = trip.getDriver().getFirstName();
                        String lastName = trip.getDriver().getLastName();
                        if (firstName != null || lastName != null) {
                            driverName = (firstName != null ? firstName : "") + 
                                         (lastName != null ? " " + lastName : "");
                            driverName = driverName.trim();
                            if (driverName.isEmpty()) driverName = null;
                        }
                    }
                    String vehicleReg = trip.getVehicle() != null ? 
                            trip.getVehicle().getRegistrationNumber() : null;
                    
                    return TripSummary.builder()
                            .tripId(trip.getId())
                            .tripNumber(trip.getTripNumber())
                            .driverName(driverName != null ? driverName : "N/A")
                            .vehicleRegistration(vehicleReg != null ? vehicleReg : "N/A")
                            .plannedStartDate(trip.getPlannedStartDate() != null ? 
                                    trip.getPlannedStartDate().format(dateFormatter) : "N/A")
                            .plannedEndDate(trip.getPlannedEndDate() != null ? 
                                    trip.getPlannedEndDate().format(dateFormatter) : "N/A")
                            .actualDistanceKm(trip.getActualDistanceKm())
                            .status(trip.getStatus())
                            .build();
                })
                .collect(Collectors.toList());

        return LoadReportDTO.builder()
                .loadNumber(load.getLoadNumber())
                .status(load.getStatus())
                .description(load.getDescription())
                .commodityType(load.getCommodityType())
                .weightKg(load.getWeightKg())
                .palletCount(load.getPalletCount())
                .tripCount(trips != null ? trips.size() : 0)
                .customerName(load.getCustomer() != null ? load.getCustomer().getName() : null)
                .totalDistanceKm(load.getTotalDistanceKm() != null ? 
                        BigDecimal.valueOf(load.getTotalDistanceKm()) : BigDecimal.ZERO)
                .totalFromDepotKm(load.getTotalFromDepotKm())
                .totalToDepotKm(load.getTotalToDepotKm())
                .totalDepotKm(load.getTotalDepotKm())
                .createdAt(load.getCreatedAt() != null ? 
                        load.getCreatedAt().format(formatter) : null)
                .updatedAt(load.getUpdatedAt() != null ? 
                        load.getUpdatedAt().format(formatter) : null)
                .trips(tripSummaries)
                .build();
    }
}
