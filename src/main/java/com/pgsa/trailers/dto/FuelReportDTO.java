// src/main/java/com/pgsa/trailers/dto/report/FuelReportDTO.java

package com.pgsa.trailers.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuelReportDTO {
    private String vehicleRegistration;
    private String startDate;
    private String endDate;
    private Double totalLiters;
    private Double totalCost;
    private Double avgUnitPrice;
    private Integer entryCount;
    private List<FuelEntry> entries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FuelEntry {
        private String date;
        private String station;
        private Double liters;
        private Double unitPrice;
        private Double total;
        private Double odometer;
    }

    public static FuelReportDTO createSample(Long vehicleId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        
        List<FuelEntry> entries = new ArrayList<>();
        entries.add(FuelEntry.builder()
                .date("22 Jan 2026 07:00")
                .station("BENONI PHOENIX DEPOT")
                .liters(253.31)
                .unitPrice(19.02)
                .total(4817.96)
                .odometer(682291.0)
                .build());
        entries.add(FuelEntry.builder()
                .date("16 Feb 2026 15:16")
                .station("YARD")
                .liters(300.0)
                .unitPrice(22.00)
                .total(6600.00)
                .odometer(100000.0)
                .build());
        entries.add(FuelEntry.builder()
                .date("01 Mar 2026 08:30")
                .station("Caltex Station")
                .liters(1400.0)
                .unitPrice(31.06)
                .total(43484.00)
                .odometer(1500.0)
                .build());

        double totalLiters = entries.stream().mapToDouble(FuelEntry::getLiters).sum();
        double totalCost = entries.stream().mapToDouble(FuelEntry::getTotal).sum();
        double avgUnitPrice = entries.stream().mapToDouble(FuelEntry::getUnitPrice).average().orElse(0.0);

        return FuelReportDTO.builder()
                .vehicleRegistration(vehicleId != null ? "ABC123GP" : "All Vehicles")
                .startDate("01 Jan 2026")
                .endDate("31 Mar 2026")
                .totalLiters(totalLiters)
                .totalCost(totalCost)
                .avgUnitPrice(avgUnitPrice)
                .entryCount(entries.size())
                .entries(entries)
                .build();
    }
}
