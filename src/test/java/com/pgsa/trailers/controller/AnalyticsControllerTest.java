package com.pgsa.trailers.controller;

import com.pgsa.trailers.repository.DriverAnalyticsRepository;
import com.pgsa.trailers.repository.TripAnalyticsRepository;
import com.pgsa.trailers.repository.VehicleAnalyticsRepository;
import com.pgsa.trailers.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsControllerTest {

    @Test
    void analyticsRepositoriesUsePortableDateFilters() throws Exception {
        assertNoDateFunction(VehicleAnalyticsRepository.class, "vehicleEfficiencyRaw");
        assertNoDateFunction(VehicleAnalyticsRepository.class, "findTopVehiclesByProfit");
        assertNoDateFunction(DriverAnalyticsRepository.class, "driverPerformanceRaw");
        assertNoDateFunction(DriverAnalyticsRepository.class, "findTopDriversByProfit");
        assertNoDateFunction(TripAnalyticsRepository.class, "findTripProfitabilityRaw");
    }

    @Test
    void getDashboardDoesNotFailWhenStatsContainNullValues() {
        AnalyticsService analyticsService = mock(AnalyticsService.class);
        AnalyticsController controller = new AnalyticsController(analyticsService);

        when(analyticsService.getDashboardSummary(any(), any()))
                .thenReturn(new AnalyticsService.DashboardSummary(List.of(), List.of(), List.of()));
        when(analyticsService.getVehicleKpis(any(), any())).thenReturn(List.of());
        when(analyticsService.getDriverKpis(any(), any())).thenReturn(List.of());
        when(analyticsService.getVehicleStats(any(), any())).thenReturn(mapWithNulls(
                "activeVehicles", "vehiclesInTrip", "vehiclesNotAvailable",
                "plannedKm", "travelledKm", "totalVehicles"
        ));
        when(analyticsService.getDriverStats(any(), any())).thenReturn(mapWithNulls(
                "activeDrivers", "driversInTrip", "driversNotAvailable",
                "driversWithPlanned", "totalDrivers", "totalTrips"
        ));
        when(analyticsService.getFuelStats(any(), any())).thenReturn(mapWithNulls(
                "totalKm", "totalFuel", "totalFuelCost", "avgEfficiency",
                "avgCostPerKm", "vehicleEfficiency", "driverEfficiency"
        ));
        when(analyticsService.getDistanceStats(any(), any())).thenReturn(mapWithNulls(
                "totalKm", "totalTrips", "completedTrips", "avgKmPerTrip"
        ));
        when(analyticsService.getTopDrivers(any(), any(), anyInt())).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.getDashboard(
                null,
                LocalDate.of(2026, 9, 27),
                LocalDate.of(2026, 10, 27)
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
        assertThat(response.getBody().get("vehicleStats")).isInstanceOf(Map.class);
        assertThat(response.getBody().get("driverStats")).isInstanceOf(Map.class);
        assertThat(response.getBody().get("fuelStats")).isInstanceOf(Map.class);
        assertThat(response.getBody().get("distanceStats")).isInstanceOf(Map.class);
    }

    private void assertNoDateFunction(Class<?> repositoryType, String methodName) throws Exception {
        Method method = java.util.Arrays.stream(repositoryType.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing method: " + methodName));
        Query query = method.getAnnotation(Query.class);
        assertThat(query).isNotNull();
        assertThat(query.value()).doesNotContain("DATE(");
    }

    private Map<String, Object> mapWithNulls(String... keys) {
        Map<String, Object> map = new HashMap<>();
        for (String key : keys) {
            map.put(key, null);
        }
        return map;
    }
}
