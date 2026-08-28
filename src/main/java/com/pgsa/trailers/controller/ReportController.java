// src/main/java/com/pgsa/trailers/controller/ReportController.java

package com.pgsa.trailers.controller;

import com.pgsa.trailers.service.JasperReportService;
import com.pgsa.trailers.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;
    private final JasperReportService jasperReportService;

    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    // ============================================================
    // HTML REPORTS
    // ============================================================

    @PostMapping(
        value = "/trip/{tripNumber}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> generateTripReportHTML(
            @PathVariable String tripNumber,
            @RequestParam(defaultValue = "html") String format) {

        log.info("📊 Generating HTML trip report for: {}", tripNumber);

        try {
            String rawHtml = reportService.generateTripReportHTML(tripNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("format", "html");
            response.put("content", rawHtml);
            response.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (Exception e) {
            log.error("❌ Error generating trip report: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage());
        }
    }

    @PostMapping(
        value = "/load/{loadNumber}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> generateLoadReportHTML(
            @PathVariable String loadNumber,
            @RequestParam(defaultValue = "html") String format) {

        log.info("📊 Generating HTML load report for: {}", loadNumber);

        try {
            String rawHtml = reportService.generateLoadReportHTML(loadNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("format", "html");
            response.put("content", rawHtml);
            response.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (Exception e) {
            log.error("❌ Error generating load report: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage());
        }
    }

    @PostMapping(
        value = "/fuel",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> generateFuelReportHTML(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "html") String format) {

        log.info("📊 Generating HTML fuel report for vehicle: {}", vehicleId);

        try {
            String rawHtml = reportService.generateFuelReportHTML(vehicleId, startDate, endDate);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("format", "html");
            response.put("content", rawHtml);
            response.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (Exception e) {
            log.error("❌ Error generating fuel report: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage());
        }
    }

    // ============================================================
    // PDF REPORTS (Jasper)
    // ============================================================

    @PostMapping(
        value = "/trip/{tripNumber}/pdf",
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> generateTripReportPDF(
            @PathVariable String tripNumber) {

        log.info("📄 Generating PDF trip report for: {}", tripNumber);

        try {
            Map<String, Object> data = reportService.getTripReportData(tripNumber);
            byte[] pdf = jasperReportService.generateTripReportPDF(data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                "trip-report-" + tripNumber + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdf);

        } catch (Exception e) {
            log.error("❌ Error generating PDF report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(
        value = "/load/{loadNumber}/pdf",
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> generateLoadReportPDF(
            @PathVariable String loadNumber) {

        log.info("📄 Generating PDF load report for: {}", loadNumber);

        try {
            Map<String, Object> data = reportService.getLoadReportData(loadNumber);
            byte[] pdf = jasperReportService.generateLoadReportPDF(data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                "load-report-" + loadNumber + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdf);

        } catch (Exception e) {
            log.error("❌ Error generating PDF report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(
        value = "/fuel/pdf",
        produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> generateFuelReportPDF(
            @RequestParam(required = false) Long vehicleId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        log.info("📄 Generating PDF fuel report for vehicle: {}", vehicleId);

        try {
            Map<String, Object> data = reportService.getFuelReportData(vehicleId, startDate, endDate);
            byte[] pdf = jasperReportService.generateFuelReportPDF(data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                "fuel-report-" + (vehicleId != null ? vehicleId : "all") + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdf);

        } catch (Exception e) {
            log.error("❌ Error generating PDF report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", message);
        errorResponse.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));

        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("birtAvailable", false);
        response.put("reportsAvailable", true);
        response.put("pdfReportsAvailable", true);
        response.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));
        return ResponseEntity.ok(response);
    }
}
