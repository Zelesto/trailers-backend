package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.TripKpiDTO;
import com.pgsa.trailers.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trip-analytics")
@RequiredArgsConstructor
public class TripAnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Get KPIs for trips between the given date range.
     * GET {{baseUrl}}/api/trip-analytics/kpis?from=YYYY-MM-DD&to=YYYY-MM-DD
     */
    @GetMapping("/kpis")
    public ResponseEntity<List<TripKpiDTO>> getTripKpis(
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        // Handle null dates with defaults
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<TripKpiDTO> kpis = analyticsService.getTripKpis(startDate, endDate);
        return ResponseEntity.ok(kpis);
    }
}