// src/main/java/com/pgsa/trailers/service/JasperReportService.java

package com.pgsa.trailers.service;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class JasperReportService {

     /**
     * Generate Trip Report as PDF
     */
    public byte[] generateTripReportPDF(String tripNumber, Map<String, Object> data) {
        try {
            log.info("📄 Generating PDF trip report for: {}", tripNumber);
            
            // Load template
            InputStream templateStream = new ClassPathResource(
                "reports/trip_report.jrxml"
            ).getInputStream();
            
            JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);
            
            // Prepare parameters
            Map<String, Object> params = new HashMap<>();
            params.put("companyName", "Trailers");
            params.put("reportTitle", "Trip Report");
            params.put("logoUrl", getLogoBase64());
            params.put("tripNumber", data.getOrDefault("tripNumber", ""));
            params.put("customerName", data.getOrDefault("customerName", ""));
            params.put("status", data.getOrDefault("status", ""));
            params.put("originLocation", data.getOrDefault("originLocation", ""));
            params.put("destinationLocation", data.getOrDefault("destinationLocation", ""));
            params.put("plannedDistanceKm", data.getOrDefault("plannedDistanceKm", 0.0));
            params.put("actualDistanceKm", data.getOrDefault("actualDistanceKm", 0.0));
            params.put("driverName", data.getOrDefault("driverName", ""));
            params.put("vehicleRegistration", data.getOrDefault("vehicleRegistration", ""));
            params.put("plannedStartDate", data.getOrDefault("plannedStartDate", ""));
            params.put("plannedEndDate", data.getOrDefault("plannedEndDate", ""));
            params.put("actualStartDate", data.getOrDefault("actualStartDate", ""));
            params.put("actualEndDate", data.getOrDefault("actualEndDate", ""));
            params.put("actualStartOdometer", data.getOrDefault("actualStartOdometer", 0.0));
            params.put("actualEndOdometer", data.getOrDefault("actualEndOdometer", 0.0));
            params.put("referenceNumber", data.getOrDefault("referenceNumber", ""));
            params.put("reportGenerated", LocalDateTime.now().format(DATE_FORMATTER));

            // Fill report
            JasperPrint jasperPrint = JasperFillManager.fillReport(
                jasperReport, 
                params, 
                new JREmptyDataSource()
            );

            // Export to PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));

            SimplePdfExporterConfiguration config = new SimplePdfExporterConfiguration();
            config.setPdfJavaScript("this.print();");
            exporter.setConfiguration(config);
            
            exporter.exportReport();

            log.info("✅ PDF trip report generated: {} bytes", outputStream.size());
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("❌ Error generating PDF report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    /**
     * Generate Trip Report as PDF using Jasper
     */
    public byte[] generateTripReportPDF(String tripNumber) {
        // Fetch trip data
        Trip trip = tripService.findByTripNumber(tripNumber);
        
        // Prepare parameters
        Map<String, Object> params = new HashMap<>();
        params.put("tripNumber", trip.getTripNumber());
        params.put("companyName", "Trailers");
        params.put("logo", getLogoBase64());
        params.put("generatedDate", new Date());
        
        // Prepare data
        List<TripData> tripData = List.of(convertToTripData(trip));
        
        return generateReport("trip_report", params, tripData);
    }

    /**
     * Generate Load Report as PDF using Jasper
     */
    public byte[] generateLoadReportPDF(String loadNumber) {
        // Similar implementation
    }

    /**
     * Generate Fuel Report as PDF using Jasper
     */
    public byte[] generateFuelReportPDF(Long vehicleId, String startDate, String endDate) {
        // Similar implementation
    }

    /**
     * Get logo as Base64 string
     */
    private String getLogoBase64() {
        // Option 1: Use a static URL
        return "https://trailers-backend.onrender.com/logo.png";
        
        // Option 2: Use Base64 encoded image
        // return "data:image/png;base64,iVBORw0KGgo...";
    }
}
Step 3:
