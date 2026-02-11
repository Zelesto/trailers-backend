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

    /** -------------------- Vehicle KPIs -------------------- */
    public List<VehicleKpiDTO> getVehicleKpis(LocalDate startDate, LocalDate endDate) {
        try {
            return vehicleRepository.vehicleEfficiencyRaw(startDate, endDate).stream()
                    .map(this::mapToVehicleKpiDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching vehicle KPIs from {} to {}", startDate, endDate, e);
            return Collections.emptyList();
        }
    }

    /** -------------------- Driver KPIs -------------------- */
    public List<DriverKpiDTO> getDriverKpis(LocalDate startDate, LocalDate endDate) {
        try {
            return driverRepository.driverPerformanceRaw().stream()
                    .map(this::mapToDriverKpiDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Dashboard driver data error for date range {} to {}", startDate, endDate, e);
            return Collections.emptyList();
        }
    }

    /** -------------------- Trip KPIs -------------------- */

    /** Get trip KPIs for a specific trip */
    public List<TripKpiDTO> getTripKpis(Long tripId) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(DEFAULT_TRIP_HISTORY_DAYS);
        return getTripKpis(start, end, tripId);
    }

    /** Get trip KPIs for a date range with LocalDateTime */
    public List<TripKpiDTO> getTripKpis(LocalDateTime start, LocalDateTime end) {
        return getTripKpis(start, end, null);
    }

    /** Get trip KPIs for a date range with LocalDate */
    public List<TripKpiDTO> getTripKpis(LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59, 999999999);
        return getTripKpis(startDateTime, endDateTime, null);
    }

    /** Get all trip KPIs for the last year */
    public List<TripKpiDTO> getAllTripKpis() {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusYears(DEFAULT_ALL_TRIPS_YEARS);
        log.info("Fetching all trip KPIs from {} to {}", start, end);
        return getTripKpis(start, end, null);
    }

    /** Internal helper to fetch trip KPIs with optional tripId filter */
    private List<TripKpiDTO> getTripKpis(LocalDateTime start, LocalDateTime end, Long tripId) {
        try {
            // Convert LocalDateTime to LocalDate for the repository method
            LocalDate startDate = start.toLocalDate();
            LocalDate endDate = end.toLocalDate();

           return tripRepository.findTripProfitabilityRaw(startDate, endDate).stream()
                    .filter(row -> tripId == null || extractTripId(row[0]) == tripId)
                    .map(this::mapToTripKpiDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching trip KPIs for date range {} to {}: {}", start, end, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /** -------------------- Dashboard Summary -------------------- */
    public DashboardSummary getDashboardSummary(LocalDate startDate, LocalDate endDate) {
        List<VehicleKpiDTO> vehicleKpis = getVehicleKpis(startDate, endDate);
        List<DriverKpiDTO> driverKpis = getDriverKpis(startDate, endDate);
        List<TripKpiDTO> tripKpis = getTripKpis(startDate, endDate);

        return new DashboardSummary(vehicleKpis, driverKpis, tripKpis);
    }

    /** -------------------- Helper Methods -------------------- */
    private VehicleKpiDTO mapToVehicleKpiDTO(Object[] row) {
        return VehicleKpiDTO.withCalculations(
                extractString(row[0]),
                toBigDecimal(row[1]),
                toBigDecimal(row[2]),
                toBigDecimal(row[3])
        );
    }

   private DriverKpiDTO mapToDriverKpiDTO(Object[] row) {
    return new DriverKpiDTO(
        extractString(row[0]), // driver_name
        toBigDecimal(row[1]),  // trips_completed
        toBigDecimal(row[2]),  // total_km
        toBigDecimal(row[3]),  // fuel_cost
        toBigDecimal(row[4]),  // efficiency_score
        toBigDecimal(row[5]),  // total_revenue
        toBigDecimal(row[6]),  // total_cost
        toBigDecimal(row[7])   // profit
    );
}
    private TripKpiDTO mapToTripKpiDTO(Object[] row) {
        return new TripKpiDTO(
                extractTripId(row[0]),
                extractString(row[1]),
                extractString(row[2]),
                extractTripDate(row[3]),
                toBigDecimal(row[4]),
                toBigDecimal(row[5]),
                toBigDecimal(row[6]),
                toBigDecimal(row[7]),
                toBigDecimal(row[8]),
                toBigDecimal(row[9])
        );
    }

    private String extractString(Object value) {
        return value != null ? value.toString() : "";
    }

    private Long extractTripId(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private LocalDate extractTripDate(Object value) {
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        } else if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        } else if (value instanceof LocalDate localDate) {
            return localDate;
        } else if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        return LocalDate.now();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            log.warn("Could not convert value '{}' to BigDecimal: {}", value, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /** -------------------- Dashboard Summary Model -------------------- */
    @RequiredArgsConstructor
    public static class DashboardSummary {
        private final List<VehicleKpiDTO> vehicleKpis;
        private final List<DriverKpiDTO> driverKpis;
        private final List<TripKpiDTO> tripKpis;

        public long getActiveVehicles() {
            return vehicleKpis.size();
        }

        public long getActiveDrivers() {
            return driverKpis.size();
        }

        public long getActiveTrips() {
            return tripKpis.size();
        }

        public BigDecimal getTotalKm() {
            return sumVehicleValues(VehicleKpiDTO::totalKm);
        }

        public BigDecimal getTotalFuelLiters() {
            return sumVehicleValues(VehicleKpiDTO::fuelLiters);
        }

        public BigDecimal getTotalFuelCost() {
            return sumVehicleValues(VehicleKpiDTO::fuelCost);
        }

        public BigDecimal getTotalDriverRevenue() {
            return sumDriverValues(DriverKpiDTO::totalRevenue);
        }

        public BigDecimal getTotalDriverProfit() {
            return sumDriverValues(DriverKpiDTO::profit);
        }

        public BigDecimal getTotalTripRevenue() {
            return sumTripValues(TripKpiDTO::revenueAmount);
        }

        public BigDecimal getTotalTripProfit() {
            return sumTripValues(TripKpiDTO::profit);
        }

        public BigDecimal getAvgFuelEfficiency() {
            return calculateAverage(vehicleKpis, VehicleKpiDTO::kmPerLiter);
        }

        public BigDecimal getAvgDriverEfficiency() {
            return calculateAverage(driverKpis, DriverKpiDTO::efficiencyScore);
        }

        // Helper methods for calculating sums and averages
        private BigDecimal sumVehicleValues(java.util.function.Function<VehicleKpiDTO, BigDecimal> extractor) {
            return vehicleKpis.stream()
                    .map(extractor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private BigDecimal sumDriverValues(java.util.function.Function<DriverKpiDTO, BigDecimal> extractor) {
            return driverKpis.stream()
                    .map(extractor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private BigDecimal sumTripValues(java.util.function.Function<TripKpiDTO, BigDecimal> extractor) {
            return tripKpis.stream()
                    .map(extractor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private <T> BigDecimal calculateAverage(List<T> items, java.util.function.Function<T, BigDecimal> extractor) {
            if (items.isEmpty()) return BigDecimal.ZERO;

            BigDecimal total = items.stream()
                    .map(extractor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return total.divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP);
        }
    }
}
