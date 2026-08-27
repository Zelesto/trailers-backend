// src/main/java/com/pgsa/trailers/controller/ReportController.java

package com.pgsa.trailers.controller;

import com.pgsa.trailers.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;

    /**
     * Check if BIRT is available
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean birtAvailable = reportService.isBirtAvailable();
            response.put("birtAvailable", birtAvailable);
            response.put("status", birtAvailable ? "BIRT available" : "BIRT unavailable, using HTML fallback");
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("birtAvailable", false);
            response.put("status", "BIRT unavailable: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Generate trip report - tries BIRT first, falls back to HTML
     */
    @PostMapping("/trip/{tripNumber}")
    public ResponseEntity<?> generateTripReport(
            @PathVariable String tripNumber,
            @RequestParam(defaultValue = "html") String format) {
        
        log.info("📊 Generating trip report for: {} (format: {})", tripNumber, format);
        
        try {
            // Try BIRT first
            if (reportService.isBirtAvailable()) {
                try {
                    byte[] reportData = reportService.generateTripReport(tripNumber, format);
                    String filename = String.format("Trip_Report_%s.%s", tripNumber, 
                            format.equals("html") ? "html" : "pdf");
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                            "inline; filename=\"" + filename + "\"");
                    headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
                    
                    MediaType mediaType = format.equals("html") 
                            ? MediaType.TEXT_HTML 
                            : MediaType.APPLICATION_PDF;
                    
                    log.info("✅ BIRT report generated successfully for: {}", tripNumber);
                    
                    return ResponseEntity.ok()
                            .headers(headers)
                            .contentType(mediaType)
                            .body(new InputStreamResource(new ByteArrayInputStream(reportData)));
                } catch (Exception e) {
                    log.warn("⚠️ BIRT generation failed, falling back to HTML: {}", e.getMessage());
                }
            }
            
            // Fallback to HTML report data
            log.info("📊 Using HTML fallback for trip: {}", tripNumber);
            Map<String, Object> htmlData = reportService.generateTripReportData(tripNumber);
            
            if (htmlData == null || htmlData.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(htmlData);
            
        } catch (Exception e) {
            log.error("❌ Failed to generate report: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }
}
