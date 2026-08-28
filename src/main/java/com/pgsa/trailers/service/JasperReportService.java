// src/main/java/com/pgsa/trailers/service/JasperReportService.java

package com.pgsa.trailers.service;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class JasperReportService {

    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private static final String LOGO_URL = 
        "https://trailers-backend.onrender.com/logo.png";

    /**
     * Generate Trip Report PDF
     */
    public byte[] generateTripReportPDF(Map<String, Object> data) {
        try {
            log.info("📄 Generating PDF trip report");
            return generatePDF("trip_report.jrxml", buildTripParams(data));
        } catch (Exception e) {
            log.error("❌ Error generating PDF trip report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF trip report", e);
        }
    }

    /**
     * Generate Load Report PDF
     */
    public byte[] generateLoadReportPDF(Map<String, Object> data) {
        try {
            log.info("📄 Generating PDF load report");
            return generatePDF("load_report.jrxml", buildLoadParams(data));
        } catch (Exception e) {
            log.error("❌ Error generating PDF load report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF load report", e);
        }
    }

    /**
     * Generate Fuel Report PDF
     */
    public byte[] generateFuelReportPDF(Map<String, Object> data) {
        try {
            log.info("📄 Generating PDF fuel report");
            return generatePDF("fuel_report.jrxml", buildFuelParams(data));
        } catch (Exception e) {
            log.error("❌ Error generating PDF fuel report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF fuel report", e);
        }
    }

    /**
     * Generic PDF generation method
     */
    private byte[] generatePDF(String templateName, Map<String, Object> params) throws JRException {
        InputStream templateStream = new ClassPathResource("reports/" + templateName).getInputStream();
        JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
        
        JasperPrint jasperPrint = JasperFillManager.fillReport(
            jasperReport, 
            params, 
            new JREmptyDataSource()
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));

        SimplePdfExporterConfiguration config = new SimplePdfExporterConfiguration();
        config.setPdfJavaScript("this.print();");
        exporter.setConfiguration(config);
        exporter.exportReport();

        log.info("✅ PDF generated: {} bytes", outputStream.size());
        return outputStream.toByteArray();
    }

    // ============================================================
    // PARAMETER BUILDERS
    // ============================================================

    private Map<String, Object> buildTripParams(Map<String, Object> data) {
        Map<String, Object> params = new HashMap<>();
        params.put("companyName", "Trailers");
        params.put("reportTitle", "Trip Report");
        params.put("logoUrl", LOGO_URL);
        params.put("tripNumber", getString(data, "tripNumber"));
        params.put("customerName", getString(data, "customerName"));
        params.put("customerCode", getString(data, "customerCode"));
        params.put("status", getString(data, "status"));
        params.put("tripType", getString(data, "tripType"));
        params.put("referenceNumber", getString(data, "referenceNumber"));
        params.put("originLocation", getString(data, "originLocation"));
        params.put("destinationLocation", getString(data, "destinationLocation"));
        params.put("plannedDistanceKm", getDouble(data, "plannedDistanceKm"));
        params.put("actualDistanceKm", getDouble(data, "actualDistanceKm"));
        params.put("driverName", getString(data, "driverName"));
        params.put("driverLicense", getString(data, "driverLicense"));
        params.put("vehicleRegistration", getString(data, "vehicleRegistration"));
        params.put("vehicleMake", getString(data, "vehicleMake"));
        params.put("vehicleModel", getString(data, "vehicleModel"));
        params.put("plannedStartDate", getString(data, "plannedStartDate"));
        params.put("plannedEndDate", getString(data, "plannedEndDate"));
        params.put("actualStartDate", getString(data, "actualStartDate"));
        params.put("actualEndDate", getString(data, "actualEndDate"));
        params.put("actualStartOdometer", getDouble(data, "actualStartOdometer"));
        params.put("actualEndOdometer", getDouble(data, "actualEndOdometer"));
        params.put("reportGenerated", LocalDateTime.now().format(DATE_FORMATTER));
        return params;
    }

    private Map<String, Object> buildLoadParams(Map<String, Object> data) {
        Map<String, Object> params = new HashMap<>();
        params.put("companyName", "Trailers");
        params.put("reportTitle", "Load Report");
        params.put("logoUrl", LOGO_URL);
        params.put("loadNumber", getString(data, "loadNumber"));
        params.put("status", getString(data, "status"));
        params.put("description", getString(data, "description"));
        params.put("commodityType", getString(data, "commodityType"));
        params.put("weightKg", getDouble(data, "weightKg"));
        params.put("palletCount", getInt(data, "palletCount"));
        params.put("tripCount", getInt(data, "tripCount"));
        params.put("customerName", getString(data, "customerName"));
        params.put("totalDistanceKm", getDouble(data, "totalDistanceKm"));
        params.put("totalFromDepotKm", getDouble(data, "totalFromDepotKm"));
        params.put("totalToDepotKm", getDouble(data, "totalToDepotKm"));
        params.put("trips", data.get("trips"));
        params.put("reportGenerated", LocalDateTime.now().format(DATE_FORMATTER));
        return params;
    }

    private Map<String, Object> buildFuelParams(Map<String, Object> data) {
        Map<String, Object> params = new HashMap<>();
        params.put("companyName", "Trailers");
        params.put("reportTitle", "Fuel Consumption Report");
        params.put("logoUrl", LOGO_URL);
        params.put("vehicleRegistration", getString(data, "vehicleRegistration"));
        params.put("startDate", getString(data, "startDate"));
        params.put("endDate", getString(data, "endDate"));
        params.put("totalLiters", getDouble(data, "totalLiters"));
        params.put("totalCost", getDouble(data, "totalCost"));
        params.put("avgUnitPrice", getDouble(data, "avgUnitPrice"));
        params.put("entryCount", getInt(data, "entryCount"));
        params.put("entries", data.get("entries"));
        params.put("reportGenerated", LocalDateTime.now().format(DATE_FORMATTER));
        return params;
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : "N/A";
    }

    private Double getDouble(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    private Integer getInt(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
}
