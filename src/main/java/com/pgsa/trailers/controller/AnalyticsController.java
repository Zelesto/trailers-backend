// src/main/java/com/pgsa/trailers/controller/AnalyticsController.java

package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.DriverKpiDTO;
import com.pgsa.trailers.dto.VehicleKpiDTO;
import com.pgsa.trailers.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/test-simple")
    public ResponseEntity<?> testSimple(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("timestamp", System.currentTimeMillis());
        response.put("authenticated", authentication != null && authentication.isAuthenticated());
        if (authentication != null) {
            response.put("username", authentication.getName());
            response.put("authorities", authentication.getAuthorities().toString());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Main dashboard endpoint - Enhanced with all metrics
     */
    @GetMapping("/dashboard")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Object>> getDashboard(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("📊 Dashboard requested by: {}", authentication != null ? authentication.getName() : "anonymous");

        // Default to current month if not specified
        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        // Get all data from service
        AnalyticsService.DashboardSummary summary = analyticsService.getDashboardSummary(startDate, endDate);
        List<VehicleKpiDTO> vehicleKpis = analyticsService.getVehicleKpis(startDate, endDate);
        List<DriverKpiDTO> driverKpis = analyticsService.getDriverKpis(startDate, endDate);

        // Get enhanced stats
        Map<String, Object> vehicleStats = analyticsService.getVehicleStats(startDate, endDate);
        Map<String, Object> driverStats = analyticsService.getDriverStats(startDate, endDate);
        Map<String, Object> fuelStats = analyticsService.getFuelStats(startDate, endDate);
        Map<String, Object> distanceStats = analyticsService.getDistanceStats(startDate, endDate);
        List<Map<String, Object>> topDrivers = analyticsService.getTopDrivers(startDate, endDate, 10);

        // Build response
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("timestamp", System.currentTimeMillis());
        response.put("period", buildSafeMap(
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "days", java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate)
        ));

        // === SUMMARY ===
        Map<String, Object> summaryData = new LinkedHashMap<>();
        summaryData.put("totalVehicles", summary.getActiveVehicles());
        summaryData.put("totalDrivers", summary.getActiveDrivers());
        summaryData.put("totalTrips", summary.getTotalTrips());
        summaryData.put("completedTrips", summary.getCompletedTrips());
        summaryData.put("totalKm", summary.getTotalKm());
        summaryData.put("totalFuelLiters", summary.getTotalFuelLiters());
        summaryData.put("totalFuelCost", summary.getTotalFuelCost());
        summaryData.put("totalRevenue", summary.getTotalDriverRevenue());
        summaryData.put("totalProfit", summary.getTotalDriverProfit());
        summaryData.put("avgFuelEfficiency", summary.getAvgFuelEfficiency());
        summaryData.put("avgCostPerKm", summary.getAvgCostPerKm());
        response.put("summary", summaryData);

        // === VEHICLE STATS ===
        response.put("vehicleStats", buildSafeMap(
                "activeVehicles", safeValue(vehicleStats, "activeVehicles"),
                "vehiclesInTrip", safeValue(vehicleStats, "vehiclesInTrip"),
                "vehiclesNotAvailable", safeValue(vehicleStats, "vehiclesNotAvailable"),
                "plannedKm", safeValue(vehicleStats, "plannedKm"),
                "travelledKm", safeValue(vehicleStats, "travelledKm"),
                "totalVehicles", safeValue(vehicleStats, "totalVehicles")
        ));

        // === DRIVER STATS ===
        response.put("driverStats", buildSafeMap(
                "activeDrivers", safeValue(driverStats, "activeDrivers"),
                "driversInTrip", safeValue(driverStats, "driversInTrip"),
                "driversNotAvailable", safeValue(driverStats, "driversNotAvailable"),
                "plannedDrivers", safeValue(driverStats, "driversWithPlanned"),
                "totalDrivers", safeValue(driverStats, "totalDrivers"),
                "totalTrips", safeValue(driverStats, "totalTrips")
        ));

        // === FUEL STATS ===
        response.put("fuelStats", buildSafeMap(
                "totalKm", safeValue(fuelStats, "totalKm"),
                "totalFuel", safeValue(fuelStats, "totalFuel"),
                "totalFuelCost", safeValue(fuelStats, "totalFuelCost"),
                "avgEfficiency", safeValue(fuelStats, "avgEfficiency"),
                "avgCostPerKm", safeValue(fuelStats, "avgCostPerKm"),
                "vehicleEfficiency", safeValue(fuelStats, "vehicleEfficiency"),
                "driverEfficiency", safeValue(fuelStats, "driverEfficiency")
        ));

        // === DISTANCE STATS ===
        response.put("distanceStats", buildSafeMap(
                "totalKm", safeValue(distanceStats, "totalKm"),
                "totalTrips", safeValue(distanceStats, "totalTrips"),
                "completedTrips", safeValue(distanceStats, "completedTrips"),
                "avgKmPerTrip", safeValue(distanceStats, "avgKmPerTrip")
        ));

        // === VEHICLE KPIs ===
        response.put("vehicleKpis", vehicleKpis.stream()
                .map(v -> buildSafeMap(
                        "registrationNumber", safeValue(v.registrationNumber(), "Unknown"),
                        "totalKm", safeValue(v.totalKm(), BigDecimal.ZERO),
                        "fuelLiters", safeValue(v.fuelLiters(), BigDecimal.ZERO),
                        "fuelCost", safeValue(v.fuelCost(), BigDecimal.ZERO),
                        "kmPerLiter", safeValue(v.kmPerLiter(), BigDecimal.ZERO),
                        "costPerKm", safeValue(v.costPerKm(), BigDecimal.ZERO)
                ))
                .collect(Collectors.toList()));

        // === DRIVER KPIs ===
        response.put("driverKpis", driverKpis.stream()
                .map(d -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("driverName", safeValue(d.driver(), "Unknown"));
                    map.put("tripsCompleted", safeValue(d.tripsCompleted(), 0));
                    map.put("totalKm", safeValue(d.totalKm(), BigDecimal.ZERO));
                    map.put("fuelCost", safeValue(d.fuelCost(), BigDecimal.ZERO));
                    map.put("totalRevenue", safeValue(d.totalRevenue(), BigDecimal.ZERO));
                    map.put("totalCost", safeValue(d.totalCost(), BigDecimal.ZERO));
                    map.put("profit", safeValue(d.profit(), BigDecimal.ZERO));
                    map.put("efficiencyScore", safeValue(d.efficiencyScore(), BigDecimal.ZERO));
                    map.put("costPerKm", safeValue(d.getCostPerKm(), BigDecimal.ZERO));
                    map.put("revenuePerTrip", safeValue(d.getRevenuePerTrip(), BigDecimal.ZERO));
                    map.put("profitMargin", safeValue(d.getProfitMargin(), BigDecimal.ZERO));
                    return map;
                })
                .collect(Collectors.toList()));

        // === TOP DRIVERS ===
        response.put("topDrivers", topDrivers);

        // === MOST EFFICIENT VEHICLE ===
        if (!vehicleKpis.isEmpty()) {
            VehicleKpiDTO mostEfficient = vehicleKpis.stream()
                    .max(Comparator.comparing(VehicleKpiDTO::kmPerLiter))
                    .orElse(vehicleKpis.get(0));
            response.put("mostEfficientVehicle", buildSafeMap(
                    "registration", safeValue(mostEfficient.registrationNumber(), "N/A"),
                    "efficiency", safeValue(mostEfficient.kmPerLiter(), BigDecimal.ZERO)
            ));
        }

        // === TOP DRIVER ===
        if (!driverKpis.isEmpty()) {
            DriverKpiDTO topDriver = driverKpis.stream()
                    .max(Comparator.comparing(DriverKpiDTO::profit))
                    .orElse(driverKpis.get(0));
            response.put("topDriver", buildSafeMap(
                    "name", safeValue(topDriver.driver(), "N/A"),
                    "profit", safeValue(topDriver.profit(), BigDecimal.ZERO),
                    "tripsCompleted", safeValue(topDriver.tripsCompleted(), 0)
            ));
        }

        log.info("✅ Dashboard response complete: {} vehicles, {} drivers, {} trips",
                vehicleKpis.size(), driverKpis.size(), summary.getTotalTrips());

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> buildSafeMap(Object... keyValuePairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            String key = String.valueOf(keyValuePairs[i]);
            Object value = keyValuePairs[i + 1];
            map.put(key, value == null ? 0 : value);
        }
        return map;
    }

    private Object safeValue(Map<String, Object> source, String key) {
        if (source == null) return 0;
        Object value = source.get(key);
        return value == null ? 0 : value;
    }

    private Object safeValue(Object value, Object fallback) {
        return value == null ? fallback : value;
    }

    @GetMapping("/vehicles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
    public ResponseEntity<Map<String, Object>> getVehicleAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (to == null) to = LocalDate.now();
        if (from == null) from = to.minusDays(30);

        List<VehicleKpiDTO> vehicleKpis = analyticsService.getVehicleKpis(from, to);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", vehicleKpis);
        response.put("count", vehicleKpis.size());
        response.put("period", Map.of("from", from, "to", to));

        // Totals
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        totals.put("totalKm", vehicleKpis.stream()
                .map(VehicleKpiDTO::totalKm)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        totals.put("totalFuelLiters", vehicleKpis.stream()
                .map(VehicleKpiDTO::fuelLiters)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        totals.put("totalFuelCost", vehicleKpis.stream()
                .map(VehicleKpiDTO::fuelCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        response.put("totals", totals);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/drivers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
    public ResponseEntity<Map<String, Object>> getDriverAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (to == null) to = LocalDate.now();
        if (from == null) from = to.minusDays(30);

        List<DriverKpiDTO> driverKpis = analyticsService.getDriverKpis(from, to);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", driverKpis);
        response.put("count", driverKpis.size());
        response.put("period", Map.of("from", from, "to", to));

        // Totals
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        totals.put("totalKm", driverKpis.stream()
                .map(DriverKpiDTO::totalKm)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        totals.put("totalRevenue", driverKpis.stream()
                .map(DriverKpiDTO::totalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        totals.put("totalProfit", driverKpis.stream()
                .map(DriverKpiDTO::profit)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        response.put("totals", totals);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "OK");
        response.put("service", "Analytics Service");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}
