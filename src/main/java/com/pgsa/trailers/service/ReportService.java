// src/main/java/com/pgsa/trailers/service/ReportService.java

package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final TripRepository tripRepository;

    /**
     * Check if BIRT is available
     */
    public boolean isBirtAvailable() {
        try {
            // Try to load BIRT classes
            Class.forName("org.eclipse.birt.core.framework.Platform");
            return true;
        } catch (ClassNotFoundException e) {
            log.debug("BIRT not available: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.debug("BIRT check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate report using BIRT
     */
    public byte[] generateTripReport(String tripNumber, String format) throws Exception {
        // This is where your BIRT generation logic goes
        // For now, throw exception to use fallback
        throw new UnsupportedOperationException("BIRT generation not yet implemented");
    }

    /**
     * Generate trip report data for HTML viewer (fallback)
     */
    public Map<String, Object> generateTripReportData(String tripNumber) {
        log.info("📊 Generating HTML report data for trip: {}", tripNumber);
        
        try {
            Trip trip = tripRepository.findByTripNumber(tripNumber)
                    .orElseThrow(() -> new RuntimeException("Trip not found: " + tripNumber));
            
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("success", true);
            reportData.put("tripNumber", trip.getTripNumber());
            reportData.put("status", trip.getStatus());
            reportData.put("originLocation", trip.getOriginLocation());
            reportData.put("destinationLocation", trip.getDestinationLocation());
            reportData.put("plannedDistanceKm", trip.getPlannedDistanceKm());
            reportData.put("actualDistanceKm", trip.getActualDistanceKm());
            reportData.put("plannedStartDate", trip.getPlannedStartDate());
            reportData.put("plannedEndDate", trip.getPlannedEndDate());
            reportData.put("actualStartDate", trip.getActualStartDate());
            reportData.put("actualEndDate", trip.getActualEndDate());
            reportData.put("actualStartOdometer", trip.getActualStartOdometer());
            reportData.put("actualEndOdometer", trip.getActualEndOdometer());
            reportData.put("actualDurationHours", trip.getActualDurationHours());
            reportData.put("referenceNumber", trip.getReferenceNumber());
            reportData.put("customerName", trip.getCustomer() != null ? trip.getCustomer().getName() : null);
            
            // Vehicle info
            if (trip.getVehicle() != null) {
                Map<String, Object> vehicle = new HashMap<>();
                vehicle.put("registrationNumber", trip.getVehicle().getRegistrationNumber());
                vehicle.put("make", trip.getVehicle().getMake());
                vehicle.put("model", trip.getVehicle().getModel());
                vehicle.put("vehicleType", trip.getVehicle().getVehicleType());
                reportData.put("vehicle", vehicle);
            }
            
            // Driver info
            if (trip.getDriver() != null) {
                Map<String, Object> driver = new HashMap<>();
                driver.put("firstName", trip.getDriver().getFirstName());
                driver.put("lastName", trip.getDriver().getLastName());
                driver.put("licenseNumber", trip.getDriver().getLicenseNumber());
                driver.put("contactNumber", trip.getDriver().getContactNumber());
                reportData.put("driver", driver);
            }
            
            log.info("✅ HTML report data generated for: {}", tripNumber);
            return reportData;
            
        } catch (Exception e) {
            log.error("❌ Failed to generate report data: {}", e.getMessage(), e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
