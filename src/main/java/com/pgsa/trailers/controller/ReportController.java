// src/main/java/com/pgsa/trailers/controller/ReportController.java

package com.pgsa.trailers.controller;

import com.pgsa.trailers.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "birtAvailable", false,
                "jasperAvailable", true
        ));
    }

    @PostMapping("/trip/{tripNumber}")
    public ResponseEntity<String> generateTripReport(
            @PathVariable String tripNumber,
            @RequestParam(defaultValue = "html") String format) {
        
        log.info("📊 Generating trip report for: {}", tripNumber);
        
        try {
            String html = reportService.generateTripReportHTML(tripNumber);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        } catch (Exception e) {
            log.error("Error generating trip report: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("<h2>Error: " + e.getMessage() + "</h2>");
        }
    }

    @PostMapping("/load/{loadNumber}")
    public ResponseEntity<String> generateLoadReport(
            @PathVariable String loadNumber,
            @RequestParam(defaultValue = "html") String format) {
        
        log.info("📊 Generating load report for: {}", loadNumber);
        
        try {
            String html = reportService.generateLoadReportHTML(loadNumber);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        } catch (Exception e) {
            log.error("Error generating load report: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("<h2>Error: " + e.getMessage() + "</h2>");
        }
    }

    @PostMapping("/fuel")
    public ResponseEntity<String> generateFuelReport(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "html") String format) {
        
        log.info("📊 Generating fuel report for vehicle: {}", vehicleId);
        
        try {
            String html = reportService.generateFuelReportHTML(vehicleId, startDate, endDate);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        } catch (Exception e) {
            log.error("Error generating fuel report: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("<h2>Error: " + e.getMessage() + "</h2>");
        }
    }
}
