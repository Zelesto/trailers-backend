// src/main/java/com/pgsa/trailers/controller/ReportController.java

package com.pgsa.trailers.controller;

import com.pgsa.trailers.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;

    @PostMapping(value = "/trip/{tripNumber}")
    public ResponseEntity<Map<String, Object>> generateTripReport(
            @PathVariable String tripNumber,
            @RequestParam(defaultValue = "html") String format) {

        log.info("📊 Generating trip report for: {}", tripNumber);

        try {
            String html = reportService.generateTripReportHTML(tripNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("format", "html");
            response.put("content", html);

            log.info("✅ Trip report generated successfully for: {}", tripNumber);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error generating trip report: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping(value = "/load/{loadNumber}")
    public ResponseEntity<Map<String, Object>> generateLoadReport(
            @PathVariable String loadNumber,
            @RequestParam(defaultValue = "html") String format) {

        log.info("📊 Generating load report for: {}", loadNumber);

        try {
            String html = reportService.generateLoadReportHTML(loadNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("format", "html");
            response.put("content", html);

            log.info("✅ Load report generated successfully for: {}", loadNumber);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error generating load report: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping(value = "/fuel")
    public ResponseEntity<Map<String, Object>> generateFuelReport(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "html") String format) {

        log.info("📊 Generating fuel report for vehicle: {}", vehicleId);

        try {
            String html = reportService.generateFuelReportHTML(vehicleId, startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("format", "html");
            response.put("content", html);

            log.info("✅ Fuel report generated successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error generating fuel report: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("birtAvailable", false);
        response.put("reportsAvailable", true);
        return ResponseEntity.ok(response);
    }
}
