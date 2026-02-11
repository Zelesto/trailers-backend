package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.VehicleKpiDTO;
import com.pgsa.trailers.dto.DriverKpiDTO;
import com.pgsa.trailers.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    /**
     * Debug endpoint to inspect current authentication
     */
    @GetMapping("/test-simple")
    public ResponseEntity<?> testSimple(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null) {
            response.put("authentication", "NULL");
        } else {
            response.put("authentication", "PRESENT");
            response.put("name", authentication.getName());
            response.put("authorities", authentication.getAuthorities().toString());
            response.put("authenticated", authentication.isAuthenticated());
        }

        // Also check SecurityContextHolder
        Authentication ctxAuth = SecurityContextHolder.getContext().getAuthentication();
        response.put("securityContextAuth", ctxAuth != null ? ctxAuth.getName() : "null");

        return ResponseEntity.ok(response);
    }
    @GetMapping("/debug/auth")
    @PreAuthorize("hasRole('SUPER_ADMIN')") // Changed from hasAuthority to hasRole
    public Map<String, Object> debugAuth(Authentication authentication) {
        log.info("DEBUG: Principal={}", authentication.getName());
        log.info("DEBUG: Authorities={}", authentication.getAuthorities());

        return Map.of(
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities(),
                "authenticated", authentication.isAuthenticated()
        );
    }

    /**
     * Main dashboard endpoint
     */
    @GetMapping("/dashboard")
   @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, Object>> getDashboard(
            Authentication authentication,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Dashboard accessed without authentication!");
            log.info("Proceeding with null authentication for debugging");
        } else {
            log.info("Dashboard access by user: {}", authentication.getName());
            log.info("User authorities: {}", authentication.getAuthorities());
        }
        // Default to last 30 days if not specified
        if (endDate == null) endDate = LocalDate.now();
        if (startDate == null) startDate = endDate.minusDays(30);

        // Get data from service
        AnalyticsService.DashboardSummary summary = analyticsService.getDashboardSummary(startDate, endDate);
        List<VehicleKpiDTO> vehicleKpis = analyticsService.getVehicleKpis(startDate, endDate);
        List<DriverKpiDTO> driverKpis = analyticsService.getDriverKpis(startDate, endDate);

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("period", Map.of("startDate", startDate, "endDate", endDate));
        response.put("timestamp", System.currentTimeMillis());

        // Summary data
        Map<String, Object> summaryData = new HashMap<>();
        summaryData.put("activeVehicles", summary.getActiveVehicles());
        summaryData.put("activeDrivers", summary.getActiveDrivers());
        summaryData.put("totalKm", summary.getTotalKm());
        summaryData.put("totalFuelLiters", summary.getTotalFuelLiters());
        summaryData.put("totalFuelCost", summary.getTotalFuelCost());
        summaryData.put("totalRevenue", summary.getTotalDriverRevenue());
        summaryData.put("totalProfit", summary.getTotalDriverProfit());
        summaryData.put("avgFuelEfficiency", summary.getAvgFuelEfficiency());
        summaryData.put("avgDriverEfficiency", summary.getAvgDriverEfficiency());
        response.put("summary", summaryData);

        // Vehicle KPIs
        List<Map<String, Object>> vehicleData = vehicleKpis.stream()
                .map(v -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("registrationNumber", v.registrationNumber());
                    map.put("totalKm", v.totalKm());
                    map.put("fuelLiters", v.fuelLiters());
                    map.put("fuelCost", v.fuelCost());
                    map.put("kmPerLiter", v.kmPerLiter());
                    map.put("costPerKm", v.costPerKm());
                    return map;
                })
                .collect(Collectors.toList());
        response.put("vehicleKpis", vehicleData);

        // Driver KPIs
        List<Map<String, Object>> driverData = driverKpis.stream()
                .map(d -> {
                    Map<String, Object> map = new HashMap<>();
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
                .collect(Collectors.toList());
        response.put("driverKpis", driverData);

        // Top performers
        if (!vehicleKpis.isEmpty()) {
            VehicleKpiDTO mostEfficientVehicle = vehicleKpis.stream()
                    .max(Comparator.comparing(VehicleKpiDTO::kmPerLiter))
                    .orElse(vehicleKpis.get(0));

            response.put("mostEfficientVehicle", Map.of(
                    "registration", mostEfficientVehicle.registrationNumber(),
                    "efficiency", mostEfficientVehicle.kmPerLiter()
            ));
        }

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

        return ResponseEntity.ok(response);
    }

    /**
     * Vehicle analytics endpoint
     */
    @GetMapping("/vehicles")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
    public ResponseEntity<Map<String, Object>> getVehicleAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order) {

        // Default to last 30 days if not specified
        if (to == null) to = LocalDate.now();
        if (from == null) from = to.minusDays(30);

        List<VehicleKpiDTO> vehicleKpis = analyticsService.getVehicleKpis(from, to);

        // Apply sorting
        if (sortBy != null) {
            vehicleKpis = sortVehicleKpis(vehicleKpis, sortBy, order);
        }

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", vehicleKpis);
        response.put("count", vehicleKpis.size());
        response.put("period", Map.of("from", from, "to", to));

        // Calculate totals
        Map<String, BigDecimal> totals = new HashMap<>();
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

    /**
     * Driver analytics endpoint
     */
    @GetMapping("/drivers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
    public ResponseEntity<Map<String, Object>> getDriverAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String order) {

        // Default to last 30 days if not specified
        if (to == null) to = LocalDate.now();
        if (from == null) from = to.minusDays(30);

        List<DriverKpiDTO> driverKpis = analyticsService.getDriverKpis(from, to);

        // Apply sorting
        if (sortBy != null) {
            driverKpis = sortDriverKpis(driverKpis, sortBy, order);
        }

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", driverKpis.stream()
                .map(d -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("driverName", d.driver());
                    map.put("tripsCompleted", d.tripsCompleted());
                    map.put("totalKm", d.totalKm());
                    map.put("fuelCost", d.fuelCost());
                    map.put("totalRevenue", d.totalRevenue());
                    map.put("totalCost", d.totalCost());
                    map.put("profit", d.profit());
                    map.put("efficiencyScore", d.efficiencyScore());
                    return map;
                })
                .collect(Collectors.toList()));
        response.put("count", driverKpis.size());
        response.put("period", Map.of("from", from, "to", to));

        return ResponseEntity.ok(response);
    }

    /**
     * Status endpoint - open to all authenticated users
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "Analytics Service");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    // Helper methods for sorting
    private List<VehicleKpiDTO> sortVehicleKpis(List<VehicleKpiDTO> kpis, String sortBy, String order) {
        boolean descending = "desc".equalsIgnoreCase(order);

        Comparator<VehicleKpiDTO> comparator;
        switch (sortBy.toLowerCase()) {
            case "efficiency":
            case "kmperliter":
                comparator = Comparator.comparing(VehicleKpiDTO::kmPerLiter);
                break;
            case "costperkm":
                comparator = Comparator.comparing(VehicleKpiDTO::costPerKm);
                break;
            case "totalkm":
                comparator = Comparator.comparing(VehicleKpiDTO::totalKm);
                break;
            case "fuelcost":
                comparator = Comparator.comparing(VehicleKpiDTO::fuelCost);
                break;
            case "registration":
            default:
                comparator = Comparator.comparing(VehicleKpiDTO::registrationNumber);
                break;
        }

        if (descending) {
            comparator = comparator.reversed();
        }

        return kpis.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    private List<DriverKpiDTO> sortDriverKpis(List<DriverKpiDTO> kpis, String sortBy, String order) {
        boolean descending = "desc".equalsIgnoreCase(order);

        Comparator<DriverKpiDTO> comparator;
        switch (sortBy.toLowerCase()) {
            case "profit":
                comparator = Comparator.comparing(DriverKpiDTO::profit);
                break;
            case "efficiency":
                comparator = Comparator.comparing(DriverKpiDTO::efficiencyScore);
                break;
            case "revenue":
                comparator = Comparator.comparing(DriverKpiDTO::totalRevenue);
                break;
            case "trips":
                comparator = Comparator.comparing(DriverKpiDTO::tripsCompleted);
                break;
            case "name":
            default:
                comparator = Comparator.comparing(DriverKpiDTO::driver);
                break;
        }

        if (descending) {
            comparator = comparator.reversed();
        }

        return kpis.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }
}
