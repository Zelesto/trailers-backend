// src/main/java/com/pgsa/trailers/controller/ReportController.java

package com.pgsa.trailers.controller;

import com.pgsa.trailers.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    @PostMapping(
    value = "/trip/{tripNumber}",
    produces = MediaType.APPLICATION_JSON_VALUE
)
public ResponseEntity<Map<String, Object>> generateTripReport(
        @PathVariable String tripNumber,
        @RequestParam(defaultValue = "html") String format) {

    log.info("📊 Generating trip report for: {}", tripNumber);

    try {
        String rawHtml = reportService.generateTripReportHTML(tripNumber);
        
        // Use JSON serialization instead of HTML escaping
        // Spring's Jackson will handle escaping automatically when serializing to JSON
        String jsonEscapedHtml = JsonUtils.escapeForJson(rawHtml);  // Or use a proper JSON library

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("format", "html");
        response.put("content", jsonEscapedHtml);

        // Alternatively, let Jackson handle it:
        // return ResponseEntity.ok(response); // Jackson handles escaping

        log.info("✅ Trip report generated successfully for: {}", tripNumber);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);

    } catch (Exception e) {
        // ... error handling
    }
}

    @PostMapping(
        value = "/load/{loadNumber}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> generateLoadReport(
            @PathVariable String loadNumber,
            @RequestParam(defaultValue = "html") String format) {

        log.info("📊 Generating load report for: {}", loadNumber);

        try {
            String rawHtml = reportService.generateLoadReportHTML(loadNumber);
            String html = HtmlUtils.htmlEscape(rawHtml);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("format", "html");
            response.put("content", html);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);


        } catch (Exception e) {
            log.error("❌ Error generating load report: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
        }
    }

    @PostMapping(
        value = "/fuel",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> generateFuelReport(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "html") String format) {

        log.info("📊 Generating fuel report for vehicle: {}", vehicleId);

        try {
            String rawHtml = reportService.generateFuelReportHTML(vehicleId, startDate, endDate);
            String html = HtmlUtils.htmlEscape(rawHtml);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("format", "html");
            response.put("content", html);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);


        } catch (Exception e) {
            log.error("❌ Error generating fuel report: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorResponse);
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
