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
        response.put("period", Map.of(
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
        response.put("vehicleStats", Map.of(
                "activeVehicles", vehicleStats.get("activeVehicles"),
                "vehiclesInTrip", vehicleStats.get("vehiclesInTrip"),
                "vehiclesNotAvailable", vehicleStats.get("vehiclesNotAvailable"),
                "plannedKm", vehicleStats.get("plannedKm"),
                "travelledKm", vehicleStats.get("travelledKm"),
                "totalVehicles", vehicleStats.get("totalVehicles")
        ));

        // === DRIVER STATS ===
        response.put("driverStats", Map.of(
                "activeDrivers", driverStats.get("activeDrivers"),
                "driversInTrip", driverStats.get("driversInTrip"),
                "driversNotAvailable", driverStats.get("driversNotAvailable"),
                "plannedDrivers", driverStats.get("driversWithPlanned"),
                "totalDrivers", driverStats.get("totalDrivers"),
                "totalTrips", driverStats.get("totalTrips")
        ));

        // === FUEL STATS ===
        response.put("fuelStats", Map.of(
                "totalKm", fuelStats.get("totalKm"),
                "totalFuel", fuelStats.get("totalFuel"),
                "totalFuelCost", fuelStats.get("totalFuelCost"),
                "avgEfficiency", fuelStats.get("avgEfficiency"),
                "avgCostPerKm", fuelStats.get("avgCostPerKm"),
                "vehicleEfficiency", fuelStats.get("vehicleEfficiency"),
                "driverEfficiency", fuelStats.get("driverEfficiency")
        ));

        // === DISTANCE STATS ===
        response.put("distanceStats", Map.of(
                "totalKm", distanceStats.get("totalKm"),
                "totalTrips", distanceStats.get("totalTrips"),
                "completedTrips", distanceStats.get("completedTrips"),
                "avgKmPerTrip", distanceStats.get("avgKmPerTrip")
        ));

        // === VEHICLE KPIs ===
        response.put("vehicleKpis", vehicleKpis.stream()
                .map(v -> Map.of(
                        "registrationNumber", v.registrationNumber(),
                        "totalKm", v.totalKm(),
                        "fuelLiters", v.fuelLiters(),
                        "fuelCost", v.fuelCost(),
                        "kmPerLiter", v.kmPerLiter(),
                        "costPerKm", v.costPerKm()
                ))
                .collect(Collectors.toList()));

        // === DRIVER KPIs ===
        response.put("driverKpis", driverKpis.stream()
                .map(d -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("driverName", d.driver());
                    map.put("tripsCompleted", d.tripsCompleted());
                    map.put("totalKm", d.totalKm());
                    map.put("fuelCost", d.fuelCost());
                    map.put("totalRevenue", d.totalRevenue());
                    map.put("totalCost", d.totalCost());
                    map.put("profit", d.profit());
                    map.put("efficiencyScore", d.efficiencyScore());
                    map.put("costPerKm", d.getCostPerKm());
                    map.put("revenuePerTrip", d.getRevenuePerTrip());
                    map.put("profitMargin", d.getProfitMargin());
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
            response.put("mostEfficientVehicle", Map.of(
                    "registration", mostEfficient.registrationNumber(),
                    "efficiency", mostEfficient.kmPerLiter()
            ));
        }

        // === TOP DRIVER ===
        if (!driverKpis.isEmpty()) {
            DriverKpiDTO topDriver = driverKpis.stream()
                    .max(Comparator.comparing(DriverKpiDTO::profit))
                    .orElse(driverKpis.get(0));
            response.put("topDriver", Map.of(
                    "name", topDriver.driver(),
                    "profit", topDriver.profit(),
                    "tripsCompleted", topDriver.tripsCompleted()
            ));
        }

        log.info("✅ Dashboard response complete: {} vehicles, {} drivers, {} trips",
                vehicleKpis.size(), driverKpis.size(), summary.getTotalTrips());

        return ResponseEntity.ok(response);
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
