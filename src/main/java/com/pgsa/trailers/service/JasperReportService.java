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
     * Generate PDF report using Jasper template
     */
    public byte[] generateReport(String templateName, Map<String, Object> parameters, List<?> data) {
        try {
            // Load JRXML template
            InputStream templateStream = new ClassPathResource("reports/" + templateName + ".jrxml").getInputStream();
            JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);

            // Create data source
            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data);

            // Fill report
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            // Export to PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JRPdfExporter exporter = new JRPdfExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));

            SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
            configuration.setPdfJavaScript("this.print();");
            exporter.setConfiguration(configuration);

            exporter.exportReport();

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error generating Jasper report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate report", e);
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

    private String getLogoBase64() {
        // Return base64 encoded logo
        return "data:image/png;base64,iVBORw0KGgo...";
    }
}
