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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    // ==================== VEHICLE KPIs ====================

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
            
            // Use the 6-parameter constructor (record has 6 fields)
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

    // ==================== DRIVER KPIs ====================

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

    // ==================== TRIP KPIs ====================

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

    // ==================== DASHBOARD SUMMARY ====================

    public DashboardSummary getDashboardSummary(LocalDate startDate, LocalDate endDate) {
        log.info("📊 Building dashboard summary from {} to {}", startDate, endDate);
        
        List<VehicleKpiDTO> vehicleKpis = getVehicleKpis(startDate, endDate);
        List<DriverKpiDTO> driverKpis = getDriverKpis(startDate, endDate);
        
        log.info("✅ Dashboard summary: {} vehicles, {} drivers", 
                vehicleKpis.size(), driverKpis.size());

        return new DashboardSummary(vehicleKpis, driverKpis);
    }

    // ==================== HELPER METHODS ====================

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

    // ==================== DASHBOARD SUMMARY MODEL ====================

    @lombok.Getter
    @lombok.RequiredArgsConstructor
    public static class DashboardSummary {
        private final List<VehicleKpiDTO> vehicleKpis;
        private final List<DriverKpiDTO> driverKpis;

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
