package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.VehicleKpiDTO;
import com.pgsa.trailers.dto.DriverKpiDTO;
import com.pgsa.trailers.dto.TripKpiDTO;
import com.pgsa.trailers.repository.TripAnalyticsRepository;
import com.pgsa.trailers.repository.VehicleAnalyticsRepository;
import com.pgsa.trailers.repository.DriverAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final TripAnalyticsRepository tripRepository;
    private final VehicleAnalyticsRepository vehicleRepository;
    private final DriverAnalyticsRepository driverRepository;

    private static final int DEFAULT_TRIP_HISTORY_DAYS = 30;
    private static final int DEFAULT_ALL_TRIPS_YEARS = 1;

    // ==================== VEHICLE KPIs ====================

    /**
     * Get vehicle performance KPIs for a date range
     */
    public List<VehicleKpiDTO> getVehicleKpis(LocalDate startDate, LocalDate endDate) {
        try {
            String fromStr = startDate.toString();
            String toStr = endDate.toString();
            
            log.info("Fetching vehicle KPIs from {} to {}", fromStr, toStr);
            
            List<Object[]> results = vehicleRepository.vehicleEfficiencyRaw(fromStr, toStr);
            log.info("Found {} vehicle records", results.size());
            
            return results.stream()
                    .map(this::mapToVehicleKpiDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching vehicle KPIs from {} to {}: {}", startDate, endDate, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // ==================== DRIVER KPIs ====================

    /**
     * Get driver performance KPIs for a date range
     */
    public List<DriverKpiDTO> getDriverKpis(LocalDate startDate, LocalDate endDate) {
        try {
            String fromStr = startDate.toString();
            String toStr = endDate.toString();
            
            log.info("Fetching driver KPIs from {} to {}", fromStr, toStr);
            
            List<Object[]> results = driverRepository.driverPerformanceRaw(fromStr, toStr);
            log.info("Found {} driver records", results.size());
            
            return results.stream()
                    .map(this::mapToDriverKpiDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching driver KPIs from {} to {}: {}", startDate, endDate, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // ==================== TRIP KPIs ====================

    /**
     * Get trip KPIs for a date range (LocalDate version)
     */
    public List<TripKpiDTO> getTripKpis(LocalDate startDate, LocalDate endDate) {
        try {
            log.info("Fetching trip KPIs from {} to {}", startDate, endDate);
            
            List<Object[]> results = tripRepository.findTripProfitabilityRaw(startDate, endDate);
            log.info("Found {} trip records", results.size());
            
            return results.stream()
                    .map(this::mapToTripKpiDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching trip KPIs from {} to {}: {}", startDate, endDate, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get trip KPIs for a specific trip ID (last 30 days)
     */
    public List<TripKpiDTO> getTripKpis(Long tripId) {
        try {
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(DEFAULT_TRIP_HISTORY_DAYS);
            
            log.info("Fetching KPIs for trip {} from {} to {}", tripId, start, end);
            
            return getTripKpis(start, end).stream()
                    .filter(trip -> trip.tripId() != null && trip.tripId().equals(tripId))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching KPIs for trip {}: {}", tripId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get all trip KPIs for the last year
     */
    public List<TripKpiDTO> getAllTripKpis() {
        try {
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusYears(DEFAULT_ALL_TRIPS_YEARS);
            
            log.info("Fetching all trip KPIs from {} to {}", start, end);
            
            return getTripKpis(start, end);
        } catch (Exception e) {
            log.error("Error fetching all trip KPIs: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // ==================== DASHBOARD SUMMARY ====================

    /**
     * Get complete dashboard summary for a date range
     */
    public DashboardSummary getDashboardSummary(LocalDate startDate, LocalDate endDate) {
        log.info("Building dashboard summary from {} to {}", startDate, endDate);
        
        List<VehicleKpiDTO> vehicleKpis = getVehicleKpis(startDate, endDate);
        List<DriverKpiDTO> driverKpis = getDriverKpis(startDate, endDate);
        List<TripKpiDTO> tripKpis = getTripKpis(startDate, endDate);
        
        log.info("Dashboard summary: {} vehicles, {} drivers, {} trips", 
                vehicleKpis.size(), driverKpis.size(), tripKpis.size());

        return new DashboardSummary(vehicleKpis, driverKpis, tripKpis);
    }

    // ==================== MAPPING METHODS ====================

    /**
     * Map database result row to VehicleKpiDTO
     * Expected column order:
     * [0] registration_number
     * [1] total_distance_km
     * [2] fuel_liters
     * [3] fuel_cost
     */
    private VehicleKpiDTO mapToVehicleKpiDTO(Object[] row) {
        try {
            return VehicleKpiDTO.withCalculations(
                    extractString(row[0]),
                    toBigDecimal(row[1]),
                    toBigDecimal(row[2]),
                    toBigDecimal(row[3])
            );
        } catch (Exception e) {
            log.error("Error mapping vehicle KPI row: {}", e.getMessage());
            return VehicleKpiDTO.withCalculations("Unknown", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

  private DriverKpiDTO mapToDriverKpiDTO(Object[] row) {
    try {
        return new DriverKpiDTO(
                extractString(row[0]),                    // driver_name
                extractInteger(row[1]),                  // ✅ trips_completed as Integer
                toBigDecimal(row[2]),                    // total_km
                toBigDecimal(row[3]),                    // fuel_cost
                toBigDecimal(row[4]),                    // efficiency_score
                toBigDecimal(row[5]),                    // total_revenue
                toBigDecimal(row[6]),                    // total_cost
                toBigDecimal(row[7])                     // profit
        );
    } catch (Exception e) {
        log.error("Error mapping driver KPI row: {}", e.getMessage());
        return new DriverKpiDTO(
                "Unknown", 
                0,                                      // ✅ Integer default
                BigDecimal.ZERO, 
                BigDecimal.ZERO, 
                BigDecimal.ZERO, 
                BigDecimal.ZERO, 
                BigDecimal.ZERO, 
                BigDecimal.ZERO
        );
    }
}

// ✅ Add this helper method
private Integer extractInteger(Object value) {
    if (value == null) return 0;
    if (value instanceof Number) return ((Number) value).intValue();
    try {
        return Integer.parseInt(value.toString().trim());
    } catch (NumberFormatException e) {
        log.warn("Could not convert '{}' to Integer: {}", value, e.getMessage());
        return 0;
    }
}

   private TripKpiDTO mapToTripKpiDTO(Object[] row) {
    try {
        return new TripKpiDTO(
            extractLong(row[0]),          // tripId
            extractString(row[1]),        // tripNumber - you need to add this to your query!
            "COMPLETED",                 // status - hardcode or add to query
            extractLocalDate(row[3]),    // plannedStartDate
            toBigDecimal(row[4]),        // totalDistanceKm
            toBigDecimal(row[8]),        // fuelUsed
            toBigDecimal(row[5]),        // revenueAmount
            toBigDecimal(row[6]),        // costAmount
            toBigDecimal(row[7]),        // profit
            BigDecimal.ZERO             // profitMargin - calculate or add to query
        );
    } catch (Exception e) {
        log.error("Error mapping trip KPI row: {}", e.getMessage());
        return new TripKpiDTO(
            0L, "", "", LocalDate.now(), 
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
    }
}
    // ==================== EXTRACTOR METHODS ====================

    private String extractString(Object value) {
        return value != null ? value.toString() : "";
    }

    private Long extractLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            log.warn("Could not convert '{}' to Long", value);
            return 0L;
        }
    }

    private LocalDate extractLocalDate(Object value) {
        if (value == null) return LocalDate.now();
        
        try {
            if (value instanceof java.sql.Timestamp timestamp) {
                return timestamp.toLocalDateTime().toLocalDate();
            } else if (value instanceof java.sql.Date date) {
                return date.toLocalDate();
            } else if (value instanceof LocalDate localDate) {
                return localDate;
            } else if (value instanceof LocalDateTime localDateTime) {
                return localDateTime.toLocalDate();
            } else if (value instanceof String) {
                return LocalDate.parse((String) value);
            }
        } catch (Exception e) {
            log.warn("Could not convert '{}' to LocalDate: {}", value, e.getMessage());
        }
        
        return LocalDate.now();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        
        try {
            if (value instanceof BigDecimal) return (BigDecimal) value;
            if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
            if (value instanceof String && !((String) value).trim().isEmpty()) {
                return new BigDecimal(((String) value).trim());
            }
        } catch (Exception e) {
            log.warn("Could not convert '{}' to BigDecimal: {}", value, e.getMessage());
        }
        
        return BigDecimal.ZERO;
    }

    // ==================== DASHBOARD SUMMARY MODEL ====================

    @RequiredArgsConstructor
    public static class DashboardSummary {
        private final List<VehicleKpiDTO> vehicleKpis;
        private final List<DriverKpiDTO> driverKpis;
        private final List<TripKpiDTO> tripKpis;

        // ========== COUNT METHODS ==========
        
        public long getActiveVehicles() {
            return vehicleKpis != null ? vehicleKpis.size() : 0L;
        }

        public long getActiveDrivers() {
            return driverKpis != null ? driverKpis.size() : 0L;
        }

        public long getActiveTrips() {
            return tripKpis != null ? tripKpis.size() : 0L;
        }

        // ========== VEHICLE TOTALS ==========
        
        public BigDecimal getTotalKm() {
            return sumValues(vehicleKpis, VehicleKpiDTO::totalKm);
        }

        public BigDecimal getTotalFuelLiters() {
            return sumValues(vehicleKpis, VehicleKpiDTO::fuelLiters);
        }

        public BigDecimal getTotalFuelCost() {
            return sumValues(vehicleKpis, VehicleKpiDTO::fuelCost);
        }

        // ========== DRIVER TOTALS ==========
        
        public BigDecimal getTotalDriverRevenue() {
            return sumValues(driverKpis, DriverKpiDTO::totalRevenue);
        }

        public BigDecimal getTotalDriverProfit() {
            return sumValues(driverKpis, DriverKpiDTO::profit);
        }

        public BigDecimal getTotalDriverCost() {
            return sumValues(driverKpis, DriverKpiDTO::totalCost);
        }

       // ========== TRIP TOTALS ==========

public BigDecimal getTotalTripRevenue() {
    return sumValues(tripKpis, TripKpiDTO::revenueAmount);
}

public BigDecimal getTotalTripProfit() {
    return sumValues(tripKpis, TripKpiDTO::profit);
}

public BigDecimal getTotalTripCost() {
    return sumValues(tripKpis, TripKpiDTO::costAmount);
}

public BigDecimal getTotalTripDistance() {
    return sumValues(tripKpis, TripKpiDTO::totalDistanceKm);  // ✅ Fixed
}

public BigDecimal getTotalFuelUsed() {
    return sumValues(tripKpis, TripKpiDTO::fuelUsed);
}

        // ========== AVERAGES ==========
        
        public BigDecimal getAvgFuelEfficiency() {
            return calculateAverage(vehicleKpis, VehicleKpiDTO::kmPerLiter);
        }

        public BigDecimal getAvgDriverEfficiency() {
            return calculateAverage(driverKpis, DriverKpiDTO::efficiencyScore);
        }

        public BigDecimal getAvgProfitPerTrip() {
            if (tripKpis == null || tripKpis.isEmpty()) return BigDecimal.ZERO;
            BigDecimal totalProfit = getTotalTripProfit();
            return totalProfit.divide(BigDecimal.valueOf(tripKpis.size()), 2, RoundingMode.HALF_UP);
        }

        public BigDecimal getAvgDistancePerTrip() {
            if (tripKpis == null || tripKpis.isEmpty()) return BigDecimal.ZERO;
            BigDecimal totalDistance = getTotalTripDistance();
            return totalDistance.divide(BigDecimal.valueOf(tripKpis.size()), 2, RoundingMode.HALF_UP);
        }

        public BigDecimal getAvgCostPerKm() {
            BigDecimal totalDistance = getTotalTripDistance();
            if (totalDistance.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
            BigDecimal totalCost = getTotalTripCost().add(getTotalFuelCost());
            return totalCost.divide(totalDistance, 2, RoundingMode.HALF_UP);
        }

        // ========== HELPER METHODS ==========
        
        private <T> BigDecimal sumValues(List<T> items, java.util.function.Function<T, BigDecimal> extractor) {
            if (items == null || items.isEmpty()) return BigDecimal.ZERO;
            return items.stream()
                    .map(extractor)
                    .filter(v -> v != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private <T> BigDecimal calculateAverage(List<T> items, java.util.function.Function<T, BigDecimal> extractor) {
            if (items == null || items.isEmpty()) return BigDecimal.ZERO;
            
            BigDecimal total = sumValues(items, extractor);
            return total.divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP);
        }
    }
}
