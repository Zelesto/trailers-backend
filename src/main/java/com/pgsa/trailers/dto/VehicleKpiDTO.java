package com.pgsa.trailers.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public record VehicleKpiDTO(
        String registrationNumber,
        BigDecimal totalKm,
        BigDecimal fuelLiters,
        BigDecimal fuelCost,
        BigDecimal kmPerLiter,
        BigDecimal costPerKm
) {
        public VehicleKpiDTO(String registrationNumber, BigDecimal totalKm, BigDecimal fuelLiters) {
                this(registrationNumber,
                        totalKm != null ? totalKm : BigDecimal.ZERO,
                        fuelLiters != null ? fuelLiters : BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO);
        }

        public static VehicleKpiDTO withCalculations(
                String registrationNumber,
                BigDecimal totalKm,
                BigDecimal fuelLiters,
                BigDecimal fuelCost) {

                if (totalKm == null) totalKm = BigDecimal.ZERO;
                if (fuelLiters == null) fuelLiters = BigDecimal.ZERO;
                if (fuelCost == null) fuelCost = BigDecimal.ZERO;

                BigDecimal kmPerLiter = BigDecimal.ZERO;
                BigDecimal costPerKm = BigDecimal.ZERO;

                if (fuelLiters.compareTo(BigDecimal.ZERO) > 0) {
                        kmPerLiter = totalKm.divide(fuelLiters, 2, RoundingMode.HALF_UP);
                }

                if (totalKm.compareTo(BigDecimal.ZERO) > 0) {
                        costPerKm = fuelCost.divide(totalKm, 4, RoundingMode.HALF_UP);
                }

                return new VehicleKpiDTO(
                        registrationNumber,
                        totalKm,
                        fuelLiters,
                        fuelCost,
                        kmPerLiter,
                        costPerKm
                );
        }

        public Map<String, Object> toMap() {
                return Map.of(
                        "registrationNumber", registrationNumber,
                        "totalKm", totalKm,
                        "fuelLiters", fuelLiters,
                        "fuelCost", fuelCost,
                        "kmPerLiter", kmPerLiter,
                        "costPerKm", costPerKm,
                        "efficiency", kmPerLiter != null ? kmPerLiter.doubleValue() : 0.0,
                        "model", "Vehicle " + registrationNumber
                );
        }
}
