// src/main/java/com/pgsa/trailers/service/AnalyticsService.java

package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.DriverKpiDTO;
import com.pgsa.trailers.dto.TripKpiDTO;
import com.pgsa.trailers.dto.VehicleKpiDTO;
import com.pgsa.trailers.repository.DriverAnalyticsRepository;
import com.pgsa.trailers.repository.TripAnalyticsRepository;
import com.pgsa.trailers.repository.VehicleAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final TripAnalyticsRepository tripRepository;
    private final VehicleAnalyticsRepository vehicleRepository;
    private final DriverAnalyticsRepository driverRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    // ============================================================
    // VEHICLE KPIs
    // ============================================================

    public List<VehicleKpiDTO> getVehicleKpis(LocalDate startDate, LocalDate endDate) {
        try {
            String fromStr = startDate.format(DATE_FORMATTER);
            String toStr = endDate.format(DATE_FORMATTER);
            
            log.info("📊 Fetching vehicle KPIs from {} to {}", fromStr, toStr);
            
            List<Object[]> results = vehicleRepository.vehicleEfficiencyRaw(fromStr, toStr);
            log.info("📊 Found {} vehicle records", results.size());
            
            return results.stream()
                    .map(this::mapToVehicleKpiDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ Error fetching vehicle KPIs: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private VehicleKpiDTO mapToVehicleKpiDTO(Object[] row) {
        try {
            String registration = row[0] != null ? row[0].toString() : "Unknown";
            BigDecimal totalKm = toBigDecimal(row[1]);
            BigDecimal fuelLiters = toBigDecimal(row[2]);
            BigDecimal fuelCost = toBigDecimal(row[3]);
            BigDecimal kmPerLiter = toBigDecimal(row[4]);
            BigDecimal costPerKm = toBigDecimal(row[5]);
            
            return new VehicleKpiDTO(
                registration,
                totalKm,
                fuelLiters,
                fuelCost,
                kmPerLiter,
                costPerKm
            );
        } catch (Exception e) {
            log.error("Error mapping vehicle KPI row: {}", e.getMessage());
            return new VehicleKpiDTO("Unknown", BigDecimal.ZERO, BigDecimal.ZERO, 
                                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    // ============================================================
    // DRIVER KPIs
    // ============================================================

    public List<DriverKpiDTO> getDriverKpis(LocalDate startDate, LocalDate endDate) {
        try {
            String fromStr = startDate.format(DATE_FORMATTER);
            String toStr = endDate.format(DATE_FORMATTER);
            
            log.info("👤 Fetching driver KPIs from {} to {}", fromStr, toStr);
            
            List<Object[]> results = driverRepository.driverPerformanceRaw(fromStr, toStr);
            log.info("👤 Found {} driver records", results.size());
            
            return results.stream()
                    .map(this::mapToDriverKpiDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ Error fetching driver KPIs: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private DriverKpiDTO mapToDriverKpiDTO(Object[] row) {
        try {
            String driverName = row[0] != null ? row[0].toString() : "Unknown";
            Integer tripsCompleted = extractInteger(row[1]);
            BigDecimal totalKm = toBigDecimal(row[2]);
            BigDecimal fuelCost = toBigDecimal(row[3]);
            BigDecimal efficiencyScore = toBigDecimal(row[4]);
            BigDecimal totalRevenue = toBigDecimal(row[5]);
            BigDecimal totalCost = toBigDecimal(row[6]);
            BigDecimal profit = toBigDecimal(row[7]);
            
            return new DriverKpiDTO(
                driverName,
                totalKm,
                fuelCost,
                tripsCompleted,
                totalRevenue,
                totalCost,
                profit,
                efficiencyScore
            );
        } catch (Exception e) {
            log.error("Error mapping driver KPI row: {}", e.getMessage());
            return new DriverKpiDTO("Unknown", BigDecimal.ZERO, BigDecimal.ZERO, 0,
                                   BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    // ============================================================
    // TRIP KPIs
    // ============================================================

    public List<TripKpiDTO> getTripKpis(LocalDate startDate, LocalDate endDate) {
        try {
            String fromStr = startDate.format(DATE_FORMATTER);
            String toStr = endDate.format(DATE_FORMATTER);
            
            log.info("📋 Fetching trip KPIs from {} to {}", fromStr, toStr);
            
            List<Object[]> results = tripRepository.findTripProfitabilityRaw(fromStr, toStr);
            log.info("📋 Found {} trip records", results.size());
            
            return results.stream()
                    .map(this::mapToTripKpiDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ Error fetching trip KPIs: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private TripKpiDTO mapToTripKpiDTO(Object[] row) {
        try {
            Long tripId = extractLong(row[0]);
            String tripNumber = row[1] != null ? row[1].toString() : "";
            String status = row[2] != null ? row[2].toString() : "";
            LocalDate plannedStartDate = extractLocalDate(row[3]);
            BigDecimal totalDistance = toBigDecimal(row[4]);
            BigDecimal revenue = toBigDecimal(row[5]);
            BigDecimal cost = toBigDecimal(row[6]);
            BigDecimal profit = toBigDecimal(row[7]);
            BigDecimal fuelUsed = toBigDecimal(row[8]);
            
            BigDecimal profitMargin = revenue.compareTo(BigDecimal.ZERO) > 0 
                ? profit.divide(revenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
            
            return new TripKpiDTO(
                tripId,
                tripNumber,
                status,
                plannedStartDate,
                totalDistance,
                fuelUsed,
                revenue,
                cost,
                profit,
                profitMargin
            );
        } catch (Exception e) {
            log.error("Error mapping trip KPI row: {}", e.getMessage());
            return new TripKpiDTO(0L, "", "", LocalDate.now(),
                                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    // ============================================================
    // DASHBOARD SUMMARY - ENHANCED
    // ============================================================

    public DashboardSummary getDashboardSummary(LocalDate startDate, LocalDate endDate) {
        log.info("📊 Building dashboard summary from {} to {}", startDate, endDate);
        
        List<VehicleKpiDTO> vehicleKpis = getVehicleKpis(startDate, endDate);
        List<DriverKpiDTO> driverKpis = getDriverKpis(startDate, endDate);
        List<TripKpiDTO> tripKpis = getTripKpis(startDate, endDate);
        
        log.info("✅ Dashboard summary: {} vehicles, {} drivers, {} trips", 
                vehicleKpis.size(), driverKpis.size(), tripKpis.size());

        return new DashboardSummary(vehicleKpis, driverKpis, tripKpis);
    }

    // ============================================================
    // VEHICLE STATS - ACTIVE VEHICLES DETAILS
    // ============================================================

    public Map<String, Object> getVehicleStats(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        // Get all vehicles - you may need to inject VehicleRepository for this
        // For now, we'll use the KPIs as a proxy
        List<VehicleKpiDTO> vehicleKpis = getVehicleKpis(startDate, endDate);
        
        // Total vehicles
        int totalVehicles = vehicleKpis.size();
        
        // Active vehicles (those with trips in the period)
        long activeVehicles = vehicleKpis.stream()
                .filter(v -> v.totalKm().compareTo(BigDecimal.ZERO) > 0)
                .count();
        
        // Vehicles with planned trips
        long vehiclesWithPlanned = vehicleKpis.stream()
                .filter(v -> v.totalKm().compareTo(BigDecimal.ZERO) == 0)
                .count();
        
        stats.put("totalVehicles", totalVehicles);
        stats.put("activeVehicles", activeVehicles);
        stats.put("vehiclesInTrip", activeVehicles);
        stats.put("vehiclesNotAvailable", totalVehicles - activeVehicles);
        stats.put("vehiclesWithPlanned", vehiclesWithPlanned);
        
        // Planned and travelled KMs
        BigDecimal plannedKm = vehicleKpis.stream()
                .map(VehicleKpiDTO::totalKm)
                .filter(km -> km != null && km.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal travelledKm = vehicleKpis.stream()
                .map(VehicleKpiDTO::totalKm)
                .filter(km -> km != null && km.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        stats.put("plannedKm", plannedKm);
        stats.put("travelledKm", travelledKm);
        
        return stats;
    }

    // ============================================================
    // DRIVER STATS - ACTIVE DRIVERS DETAILS
    // ============================================================

    public Map<String, Object> getDriverStats(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        List<DriverKpiDTO> driverKpis = getDriverKpis(startDate, endDate);
        
        int totalDrivers = driverKpis.size();
        long activeDrivers = driverKpis.stream()
                .filter(d -> d.tripsCompleted() > 0)
                .count();
        long driversWithPlanned = driverKpis.stream()
                .filter(d -> d.tripsCompleted() == 0)
                .count();
        
        stats.put("totalDrivers", totalDrivers);
        stats.put("activeDrivers", activeDrivers);
        stats.put("driversInTrip", activeDrivers);
        stats.put("driversNotAvailable", totalDrivers - activeDrivers);
        stats.put("driversWithPlanned", driversWithPlanned);
        
        // Trip count per driver
        Map<String, Integer> tripCountByDriver = driverKpis.stream()
                .collect(Collectors.toMap(
                        DriverKpiDTO::driver,
                        DriverKpiDTO::tripsCompleted,
                        (a, b) -> a
                ));
        stats.put("tripCountByDriver", tripCountByDriver);
        
        // Total trips
        int totalTrips = driverKpis.stream()
                .mapToInt(DriverKpiDTO::tripsCompleted)
                .sum();
        stats.put("totalTrips", totalTrips);
        
        return stats;
    }

    // ============================================================
    // FUEL EFFICIENCY STATS
    // ============================================================

    public Map<String, Object> getFuelStats(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        List<VehicleKpiDTO> vehicleKpis = getVehicleKpis(startDate, endDate);
        List<DriverKpiDTO> driverKpis = getDriverKpis(startDate, endDate);
        
        // Total KM and Fuel
        BigDecimal totalKm = vehicleKpis.stream()
                .map(VehicleKpiDTO::totalKm)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalFuel = vehicleKpis.stream()
                .map(VehicleKpiDTO::fuelLiters)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalFuelCost = vehicleKpis.stream()
                .map(VehicleKpiDTO::fuelCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Average efficiency (km/L)
        BigDecimal avgEfficiency = BigDecimal.ZERO;
        if (totalFuel.compareTo(BigDecimal.ZERO) > 0 && totalKm.compareTo(BigDecimal.ZERO) > 0) {
            avgEfficiency = totalKm.divide(totalFuel, 2, RoundingMode.HALF_UP);
        }
        
        // Average cost per km
        BigDecimal avgCostPerKm = BigDecimal.ZERO;
        if (totalKm.compareTo(BigDecimal.ZERO) > 0 && totalFuelCost.compareTo(BigDecimal.ZERO) > 0) {
            avgCostPerKm = totalFuelCost.divide(totalKm, 2, RoundingMode.HALF_UP);
        }
        
        // Per vehicle efficiency
        Map<String, BigDecimal> vehicleEfficiency = vehicleKpis.stream()
                .filter(v -> v.kmPerLiter() != null && v.kmPerLiter().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toMap(
                        VehicleKpiDTO::registrationNumber,
                        VehicleKpiDTO::kmPerLiter,
                        (a, b) -> a
                ));
        
        // Per driver efficiency
        Map<String, BigDecimal> driverEfficiency = driverKpis.stream()
                .filter(d -> d.efficiencyScore() != null && d.efficiencyScore().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toMap(
                        DriverKpiDTO::driver,
                        DriverKpiDTO::efficiencyScore,
                        (a, b) -> a
                ));
        
        // Per driver cost per km
        Map<String, BigDecimal> driverCostPerKm = driverKpis.stream()
                .filter(d -> d.getCostPerKm() != null && d.getCostPerKm().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toMap(
                        DriverKpiDTO::driver,
                        DriverKpiDTO::getCostPerKm,
                        (a, b) -> a
                ));
        
        stats.put("totalKm", totalKm);
        stats.put("totalFuel", totalFuel);
        stats.put("totalFuelCost", totalFuelCost);
        stats.put("avgEfficiency", avgEfficiency);
        stats.put("avgCostPerKm", avgCostPerKm);
        stats.put("vehicleEfficiency", vehicleEfficiency);
        stats.put("driverEfficiency", driverEfficiency);
        stats.put("driverCostPerKm", driverCostPerKm);
        stats.put("tripsWithFuelData", vehicleKpis.stream()
                .filter(v -> v.fuelLiters() != null && v.fuelLiters().compareTo(BigDecimal.ZERO) > 0)
                .count());
        
        return stats;
    }

    // ============================================================
    // DISTANCE STATS
    // ============================================================

    public Map<String, Object> getDistanceStats(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        List<VehicleKpiDTO> vehicleKpis = getVehicleKpis(startDate, endDate);
        List<TripKpiDTO> tripKpis = getTripKpis(startDate, endDate);
        
        // Total KM travelled
        BigDecimal totalKm = vehicleKpis.stream()
                .map(VehicleKpiDTO::totalKm)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Total trips
        int totalTrips = tripKpis.size();
        long completedTrips = tripKpis.stream()
                .filter(t -> "COMPLETED".equals(t.status()) || "FINALIZED".equals(t.status()))
                .count();
        
        // Average km per trip
        BigDecimal avgKmPerTrip = BigDecimal.ZERO;
        if (totalTrips > 0) {
            avgKmPerTrip = totalKm.divide(BigDecimal.valueOf(totalTrips), 2, RoundingMode.HALF_UP);
        }
        
        stats.put("totalKm", totalKm);
        stats.put("totalTrips", totalTrips);
        stats.put("completedTrips", completedTrips);
        stats.put("avgKmPerTrip", avgKmPerTrip);
        
        return stats;
    }

    // ============================================================
    // TOP PERFORMING DRIVERS
    // ============================================================

    public List<Map<String, Object>> getTopDrivers(LocalDate startDate, LocalDate endDate, int limit) {
        List<DriverKpiDTO> driverKpis = getDriverKpis(startDate, endDate);
        
        return driverKpis.stream()
                .filter(d -> d.tripsCompleted() > 0)
                .map(d -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("name", d.driver());
                    map.put("tripsCompleted", d.tripsCompleted());
                    map.put("efficiency", d.efficiencyScore());
                    map.put("costPerKm", d.getCostPerKm());
                    map.put("rating", calculateRating(d));
                    map.put("profit", d.profit());
                    map.put("totalKm", d.totalKm());
                    return map;
                })
                .sorted((a, b) -> {
                    // Sort by rating descending
                    Double ratingA = (Double) a.get("rating");
                    Double ratingB = (Double) b.get("rating");
                    return ratingB.compareTo(ratingA);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    private double calculateRating(DriverKpiDTO d) {
        double rating = 3.0; // Base
        
        // Efficiency factor (0-1.5 points)
        if (d.efficiencyScore() != null && d.efficiencyScore().compareTo(BigDecimal.ZERO) > 0) {
            double eff = d.efficiencyScore().doubleValue();
            rating += Math.min(eff / 10.0, 1.5);
        }
        
        // Trip count factor (0-0.5 points)
        rating += Math.min(d.tripsCompleted() / 20.0, 0.5);
        
        // Profit factor (0-0.5 points)
        if (d.profit() != null && d.profit().compareTo(BigDecimal.ZERO) > 0) {
            rating += Math.min(d.profit().doubleValue() / 10000.0, 0.5);
        }
        
        return Math.min(rating, 5.0);
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        try {
            if (value instanceof BigDecimal) return (BigDecimal) value;
            if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
            if (value instanceof String && !((String) value).trim().isEmpty()) {
                return new BigDecimal(((String) value).trim());
            }
        } catch (Exception e) {
            log.warn("Could not convert '{}' to BigDecimal", value);
        }
        return BigDecimal.ZERO;
    }

    private Integer extractInteger(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Long extractLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
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
            } else if (value instanceof LocalDate date) {
                return date;
            } else if (value instanceof String str) {
                return LocalDate.parse(str);
            }
        } catch (Exception e) {
            log.warn("Could not convert '{}' to LocalDate", value);
        }
        return LocalDate.now();
    }

    // ============================================================
    // DASHBOARD SUMMARY MODEL
    // ============================================================

    @lombok.Getter
    @lombok.RequiredArgsConstructor
    public static class DashboardSummary {
        private final List<VehicleKpiDTO> vehicleKpis;
        private final List<DriverKpiDTO> driverKpis;
        private final List<TripKpiDTO> tripKpis;

        public long getActiveVehicles() {
            return vehicleKpis != null ? vehicleKpis.size() : 0L;
        }

        public long getActiveDrivers() {
            return driverKpis != null ? driverKpis.size() : 0L;
        }

        public BigDecimal getTotalKm() {
            return sumValues(vehicleKpis, VehicleKpiDTO::totalKm);
        }

        public BigDecimal getTotalFuelLiters() {
            return sumValues(vehicleKpis, VehicleKpiDTO::fuelLiters);
        }

        public BigDecimal getTotalFuelCost() {
            return sumValues(vehicleKpis, VehicleKpiDTO::fuelCost);
        }

        public BigDecimal getTotalDriverRevenue() {
            return sumValues(driverKpis, DriverKpiDTO::totalRevenue);
        }

        public BigDecimal getTotalDriverProfit() {
            return sumValues(driverKpis, DriverKpiDTO::profit);
        }

        public BigDecimal getAvgFuelEfficiency() {
            return calculateAverage(vehicleKpis, VehicleKpiDTO::kmPerLiter);
        }

        public BigDecimal getAvgDriverEfficiency() {
            return calculateAverage(driverKpis, DriverKpiDTO::efficiencyScore);
        }

        public int getTotalTrips() {
            return tripKpis != null ? tripKpis.size() : 0;
        }

        public long getCompletedTrips() {
            if (tripKpis == null) return 0;
            return tripKpis.stream()
                    .filter(t -> "COMPLETED".equals(t.status()) || "FINALIZED".equals(t.status()))
                    .count();
        }

        public BigDecimal getAvgCostPerKm() {
            BigDecimal totalKm = getTotalKm();
            BigDecimal totalCost = getTotalDriverCost();
            if (totalKm.compareTo(BigDecimal.ZERO) > 0 && totalCost.compareTo(BigDecimal.ZERO) > 0) {
                return totalCost.divide(totalKm, 2, RoundingMode.HALF_UP);
            }
            return BigDecimal.ZERO;
        }

        public BigDecimal getTotalDriverCost() {
            return sumValues(driverKpis, DriverKpiDTO::totalCost);
        }

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
