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
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/trip/{tripNumber}")
    public ResponseEntity<InputStreamResource> generateTripReport(
            @PathVariable String tripNumber,
            @RequestParam(defaultValue = "html") String format) {
        
        log.info("📊 Generating trip report for: {}", tripNumber);
        
        try {
            byte[] reportData = reportService.generateTripReport(tripNumber, format);
            
            String filename = String.format("Trip_Report_%s.%s", tripNumber, 
                    format.equals("html") ? "html" : "pdf");
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                    "inline; filename=\"" + filename + "\"");
            
            MediaType mediaType = format.equals("html") 
                    ? MediaType.TEXT_HTML 
                    : MediaType.APPLICATION_PDF;
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(mediaType)
                    .body(new InputStreamResource(new ByteArrayInputStream(reportData)));
            
        } catch (Exception e) {
            log.error("❌ Failed to generate report: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}
