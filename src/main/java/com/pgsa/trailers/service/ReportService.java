// src/main/java/com/pgsa/trailers/service/ReportService.java

package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final TripRepository tripRepository;

    /**
     * Generate trip report in specified format
     */
    public byte[] generateTripReport(String tripNumber, String format) throws Exception {
        log.info("📊 Generating JasperReport for trip: {} (format: {})", tripNumber, format);

        // Fetch trip data
        Trip trip = tripRepository.findByTripNumber(tripNumber)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + tripNumber));

        // Prepare report data
        Map<String, Object> params = new HashMap<>();
        params.put("tripNumber", trip.getTripNumber());
        params.put("customerName", trip.getCustomer() != null ? trip.getCustomer().getName() : "N/A");
        params.put("originLocation", trip.getOriginLocation() != null ? trip.getOriginLocation() : "N/A");
        params.put("destinationLocation", trip.getDestinationLocation() != null ? trip.getDestinationLocation() : "N/A");
        params.put("plannedDistanceKm", trip.getPlannedDistanceKm() != null ? trip.getPlannedDistanceKm().doubleValue() : 0);
        params.put("actualDistanceKm", trip.getActualDistanceKm() != null ? trip.getActualDistanceKm().doubleValue() : 0);
        params.put("status", trip.getStatus() != null ? trip.getStatus() : "N/A");
        params.put("driverName", trip.getDriver() != null ?
                (trip.getDriver().getFirstName() + " " + trip.getDriver().getLastName()) : "N/A");
        params.put("vehicleRegistration", trip.getVehicle() != null ?
                trip.getVehicle().getRegistrationNumber() : "N/A");
        params.put("plannedStartDate", trip.getPlannedStartDate() != null ?
                trip.getPlannedStartDate().toString() : "N/A");
        params.put("plannedEndDate", trip.getPlannedEndDate() != null ?
                trip.getPlannedEndDate().toString() : "N/A");
        params.put("actualStartDate", trip.getActualStartDate() != null ?
                trip.getActualStartDate().toString() : "N/A");
        params.put("actualEndDate", trip.getActualEndDate() != null ?
                trip.getActualEndDate().toString() : "N/A");
        params.put("actualStartOdometer", trip.getActualStartOdometer() != null ?
                trip.getActualStartOdometer().doubleValue() : 0);
        params.put("actualEndOdometer", trip.getActualEndOdometer() != null ?
                trip.getActualEndOdometer().doubleValue() : 0);
        params.put("referenceNumber", trip.getReferenceNumber() != null ? trip.getReferenceNumber() : "N/A");
        params.put("createdAt", trip.getCreatedAt() != null ? trip.getCreatedAt().toString() : "N/A");
        params.put("reportGenerated", new Date().toString());

        // Report title with logo placeholder
        params.put("companyName", "PGS Trailers");
        params.put("reportTitle", "Trip Report");

        // Load report template - use compiled jasper if available, otherwise compile
        JasperReport jasperReport;
        try {
            // Try to load compiled .jasper file first
            InputStream jasperStream = new ClassPathResource("reports/trip_report.jasper").getInputStream();
            jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);
            log.info("✅ Loaded compiled report template");
        } catch (Exception e) {
            // Fallback: compile from .jrxml
            log.info("Compiling report template from JRXML...");
            InputStream reportStream = new ClassPathResource("reports/trip_report.jrxml").getInputStream();
            jasperReport = JasperCompileManager.compileReport(reportStream);
        }

        // Create data source
        List<Map<String, Object>> dataList = Collections.singletonList(params);
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(dataList);

        // Fill report
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);

        // Export based on format
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        switch (format.toLowerCase()) {
            case "pdf":
                exportToPDF(jasperPrint, outputStream);
                break;
            case "xlsx":
                exportToExcel(jasperPrint, outputStream);
                break;
            case "html":
            default:
                exportToHTML(jasperPrint, outputStream);
        }

        log.info("✅ JasperReport generated successfully for: {}", tripNumber);
        return outputStream.toByteArray();
    }

    private void exportToPDF(JasperPrint jasperPrint, ByteArrayOutputStream outputStream) throws JRException {
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        
        SimplePdfExporterConfiguration config = new SimplePdfExporterConfiguration();
        config.setMetadataTitle("Trip Report");
        config.setMetadataAuthor("PGS Trailers");
        exporter.setConfiguration(config);
        
        exporter.exportReport();
    }

    private void exportToHTML(JasperPrint jasperPrint, ByteArrayOutputStream outputStream) throws JRException {
        // ✅ Use HtmlExporter instead of JRHtmlExporter (newer API)
        HtmlExporter exporter = new HtmlExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleHtmlExporterOutput(outputStream));
        
        SimpleHtmlExporterConfiguration config = new SimpleHtmlExporterConfiguration();
        config.setBetweenPagesHtml("");
        exporter.setConfiguration(config);
        
        exporter.exportReport();
    }

    private void exportToExcel(JasperPrint jasperPrint, ByteArrayOutputStream outputStream) throws JRException {
        JRXlsxExporter exporter = new JRXlsxExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        
        SimpleXlsxExporterConfiguration config = new SimpleXlsxExporterConfiguration();
        config.setSheetNames(new String[]{"Trip Report"});
        exporter.setConfiguration(config);
        
        exporter.exportReport();
    }
}
