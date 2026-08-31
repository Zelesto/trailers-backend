// src/main/java/com/pgsa/trailers/service/ReportService.java

package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.report.FuelReportDTO;
import com.pgsa.trailers.dto.report.LoadReportDTO;
import com.pgsa.trailers.dto.report.TripReportDTO;
import com.pgsa.trailers.entity.ops.FuelSlip;
import com.pgsa.trailers.entity.ops.FuelSource;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.repository.FuelSlipRepository;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final TripRepository tripRepository;
    private final LoadRepository loadRepository;
    // ✅ USE FuelSlipRepository
    private final FuelSlipRepository fuelSlipRepository;
    private final VehicleRepository vehicleRepository;

    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    // ✅ LOGO URL
    private static final String LOGO_URL = "https://trailers-backend.onrender.com/logo.png";

    // ============================================================
    // TRIP REPORT
    // ============================================================

    @Transactional(readOnly = true)
    public String generateTripReportHTML(String tripNumber) {
        log.info("📊 Generating trip report for: {}", tripNumber);

        try {
            Trip trip = tripRepository.findByTripNumberWithRelations(tripNumber)
                    .orElseThrow(() -> new RuntimeException("Trip not found: " + tripNumber));

            TripReportDTO reportDTO = TripReportDTO.fromEntity(trip);
            return generateTripHTML(reportDTO);
            
        } catch (Exception e) {
            log.error("Error generating trip report: {}", e.getMessage(), e);
            return generateErrorHTML("Trip Report", tripNumber, e.getMessage());
        }
    }

    // ============================================================
    // LOAD REPORT
    // ============================================================

    @Transactional(readOnly = true)
public String generateLoadReportHTML(String loadNumber) {
    log.info("📊 Generating load report for: {}", loadNumber);

    try {
        // ✅ Check if load exists
        Optional<Load> loadOpt = loadRepository.findByLoadNumber(loadNumber);
        if (loadOpt.isEmpty()) {
            String error = "Load not found: " + loadNumber;
            log.error("❌ {}", error);
            return generateErrorHTML("Load Report", loadNumber, error);
        }
        
        Load load = loadOpt.get();
        log.info("✅ Load found: {}", load.getLoadNumber());
        
        // ✅ Get trips for this load
        List<Trip> trips = tripRepository.findByLoadId(load.getLoadNumber());
        log.info("📊 Found {} trips for load {}", trips.size(), load.getLoadNumber());
        
        // ✅ Build DTO
        LoadReportDTO reportDTO = LoadReportDTO.fromEntity(load, trips);
        
        // ✅ Generate HTML
        String html = generateLoadHTML(reportDTO);
        
        // ✅ Log the HTML length to verify
        log.info("📊 Generated load report HTML length: {} bytes", html != null ? html.length() : 0);
        
        return html;
        
    } catch (Exception e) {
        log.error("❌ Error generating load report: {}", e.getMessage(), e);
        return generateErrorHTML("Load Report", loadNumber, e.getMessage());
    }
}

    // ============================================================
    // FUEL REPORT - ✅ FIXED TO USE FuelSlip
    // ============================================================

    @Transactional(readOnly = true)
    public String generateFuelReportHTML(Long vehicleId, String startDateStr, String endDateStr) {
        log.info("📊 Generating fuel report - vehicle: {}, start: {}, end: {}", 
            vehicleId, startDateStr, endDateStr);

        try {
            // Parse dates
            LocalDate startDate = parseDate(startDateStr);
            LocalDate endDate = parseDate(endDateStr);

            // ✅ Query database for fuel slips
            List<FuelSlip> slips = getFuelSlips(vehicleId, startDate, endDate);
            
            log.info("📊 Found {} fuel slips", slips.size());

            // ✅ Build DTO from database results
            FuelReportDTO reportDTO = buildFuelReportDTO(slips, vehicleId, startDate, endDate);
            
            return generateFuelHTML(reportDTO);
            
        } catch (Exception e) {
            log.error("❌ Error generating fuel report: {}", e.getMessage(), e);
            return generateErrorHTML("Fuel Report", 
                String.valueOf(vehicleId), e.getMessage());
        }
    }

    // ============================================================
    // FUEL DATA QUERY METHODS - ✅ UPDATED FOR FuelSlip
    // ============================================================

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            log.warn("⚠️ Invalid date format: {}, using null", dateStr);
            return null;
        }
    }

    private List<FuelSlip> getFuelSlips(Long vehicleId, LocalDate startDate, LocalDate endDate) {
        // Convert LocalDate to LocalDateTime (start/end of day)
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;

        // ✅ Use FuelSlipRepository methods
        if (vehicleId != null && startDateTime != null && endDateTime != null) {
            return fuelSlipRepository.findByVehicleIdAndDateBetween(vehicleId, startDateTime, endDateTime);
        } else if (vehicleId != null) {
            return fuelSlipRepository.findByVehicleId(vehicleId);
        } else if (startDateTime != null && endDateTime != null) {
            return fuelSlipRepository.findByTransactionDateBetween(startDateTime, endDateTime);
        } else {
            // Return all slips if no filters
            return fuelSlipRepository.findAll();
        }
    }

    // src/main/java/com/pgsa/trailers/service/ReportService.java

private FuelReportDTO buildFuelReportDTO(List<FuelSlip> slips, Long vehicleId, 
                                         LocalDate startDate, LocalDate endDate) {
    // Get vehicle info
    String vehicleRegistration = null;
    if (vehicleId != null) {
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
        if (vehicleOpt.isPresent()) {
            vehicleRegistration = vehicleOpt.get().getRegistrationNumber();
        }
    }

    // Build DTO
    FuelReportDTO dto = new FuelReportDTO();
    dto.setVehicleRegistration(vehicleRegistration != null ? vehicleRegistration : "All Vehicles");
    dto.setStartDate(startDate != null ? startDate.format(DATE_FORMATTER) : "N/A");
    dto.setEndDate(endDate != null ? endDate.format(DATE_FORMATTER) : "N/A");
    dto.setEntryCount(slips.size());

    // ✅ Calculate totals using streams (no lambda mutation issues)
    double totalLiters = slips.stream()
        .filter(s -> s.getQuantity() != null)
        .mapToDouble(s -> s.getQuantity().doubleValue())
        .sum();

    double totalCost = slips.stream()
        .filter(s -> s.getTotalAmount() != null)
        .mapToDouble(s -> s.getTotalAmount().doubleValue())
        .sum();

    // ✅ Calculate average unit price
    double avgUnitPrice = slips.stream()
        .filter(s -> s.getUnitPrice() != null)
        .mapToDouble(s -> s.getUnitPrice().doubleValue())
        .average()
        .orElse(0.0);

    // ✅ Build entry list
    List<FuelReportDTO.FuelEntry> fuelEntryList = new ArrayList<>();
    for (FuelSlip slip : slips) {
        FuelReportDTO.FuelEntry dtoEntry = new FuelReportDTO.FuelEntry();
        
        // Format date
        dtoEntry.setDate(slip.getTransactionDate() != null ? 
            slip.getTransactionDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "N/A");
        
        // ✅ Use getName() from FuelSource
        dtoEntry.setStation(slip.getFuelSource() != null ? 
            slip.getFuelSource().getName() : "N/A");
        
        // Get values from FuelSlip
        Double liters = slip.getQuantity() != null ? slip.getQuantity().doubleValue() : 0.0;
        Double cost = slip.getTotalAmount() != null ? slip.getTotalAmount().doubleValue() : 0.0;
        Double pricePerLiter = slip.getUnitPrice() != null ? slip.getUnitPrice().doubleValue() : 0.0;
        
        dtoEntry.setLiters(liters);
        dtoEntry.setTotal(cost);
        dtoEntry.setUnitPrice(pricePerLiter);
        dtoEntry.setOdometer(slip.getOdometerReading() != null ? 
            slip.getOdometerReading().doubleValue() : 0.0);
        
        fuelEntryList.add(dtoEntry);
    }

    // Set values
    dto.setTotalLiters(totalLiters);
    dto.setTotalCost(totalCost);
    dto.setAvgUnitPrice(avgUnitPrice);
    dto.setEntries(fuelEntryList);

    log.info("📊 Fuel report DTO built: {} entries, total liters: {}, total cost: {}", 
        fuelEntryList.size(), totalLiters, totalCost);

    return dto;
}

    // ============================================================
    // DATA METHODS FOR PDF REPORTS (Jasper) - ✅ UPDATED
    // ============================================================

    public Map<String, Object> getTripReportData(String tripNumber) {
        log.info("📊 Getting trip report data for: {}", tripNumber);
        
        try {
            Trip trip = tripRepository.findByTripNumberWithRelations(tripNumber)
                    .orElseThrow(() -> new RuntimeException("Trip not found: " + tripNumber));
            
            TripReportDTO dto = TripReportDTO.fromEntity(trip);
            Map<String, Object> data = new HashMap<>();
            
            data.put("tripNumber", dto.getTripNumber());
            data.put("customerName", dto.getCustomerName());
            data.put("customerCode", dto.getCustomerCode());
            data.put("status", dto.getStatus());
            data.put("tripType", dto.getTripType());
            data.put("referenceNumber", dto.getReferenceNumber());
            data.put("originLocation", dto.getOriginLocation());
            data.put("destinationLocation", dto.getDestinationLocation());
            data.put("plannedDistanceKm", dto.getPlannedDistanceKm());
            data.put("actualDistanceKm", dto.getActualDistanceKm());
            data.put("driverName", dto.getDriverName());
            data.put("driverLicense", dto.getDriverLicense());
            data.put("vehicleRegistration", dto.getVehicleRegistration());
            data.put("vehicleMake", dto.getVehicleMake());
            data.put("vehicleModel", dto.getVehicleModel());
            data.put("plannedStartDate", dto.getPlannedStartDate());
            data.put("plannedEndDate", dto.getPlannedEndDate());
            data.put("actualStartDate", dto.getActualStartDate());
            data.put("actualEndDate", dto.getActualEndDate());
            data.put("actualStartOdometer", dto.getActualStartOdometer());
            data.put("actualEndOdometer", dto.getActualEndOdometer());
            
            return data;
            
        } catch (Exception e) {
            log.error("Error getting trip report data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get trip report data", e);
        }
    }

    public Map<String, Object> getLoadReportData(String loadNumber) {
        log.info("📊 Getting load report data for: {}", loadNumber);
        
        try {
            Load load = loadRepository.findByLoadNumber(loadNumber)
                    .orElseThrow(() -> new RuntimeException("Load not found: " + loadNumber));
            
            List<Trip> trips = tripRepository.findByLoadId(load.getLoadNumber());
            LoadReportDTO dto = LoadReportDTO.fromEntity(load, trips);
            
            Map<String, Object> data = new HashMap<>();
            data.put("loadNumber", dto.getLoadNumber());
            data.put("status", dto.getStatus());
            data.put("description", dto.getDescription());
            data.put("commodityType", dto.getCommodityType());
            data.put("weightKg", dto.getWeightKg());
            data.put("palletCount", dto.getPalletCount());
            data.put("tripCount", dto.getTripCount());
            data.put("customerName", dto.getCustomerName());
            data.put("totalDistanceKm", dto.getTotalDistanceKm());
            data.put("totalFromDepotKm", dto.getTotalFromDepotKm());
            data.put("totalToDepotKm", dto.getTotalToDepotKm());
            
            List<Map<String, Object>> tripList = new ArrayList<>();
            for (LoadReportDTO.TripSummary trip : dto.getTrips()) {
                Map<String, Object> tripMap = new HashMap<>();
                tripMap.put("tripNumber", trip.getTripNumber());
                tripMap.put("driverName", trip.getDriverName());
                tripMap.put("vehicleRegistration", trip.getVehicleRegistration());
                tripMap.put("plannedStartDate", trip.getPlannedStartDate());
                tripMap.put("plannedEndDate", trip.getPlannedEndDate());
                tripMap.put("actualDistanceKm", trip.getActualDistanceKm());
                tripMap.put("status", trip.getStatus());
                tripList.add(tripMap);
            }
            data.put("trips", tripList);
            
            return data;
            
        } catch (Exception e) {
            log.error("Error getting load report data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get load report data", e);
        }
    }

    public Map<String, Object> getFuelReportData(Long vehicleId, String startDate, String endDate) {
        log.info("📊 Getting fuel report data for PDF: vehicle: {}", vehicleId);
        
        try {
            LocalDate fromDate = parseDate(startDate);
            LocalDate toDate = parseDate(endDate);
            
            List<FuelSlip> slips = getFuelSlips(vehicleId, fromDate, toDate);
            FuelReportDTO dto = buildFuelReportDTO(slips, vehicleId, fromDate, toDate);
            
            Map<String, Object> data = new HashMap<>();
            data.put("vehicleRegistration", dto.getVehicleRegistration());
            data.put("startDate", dto.getStartDate());
            data.put("endDate", dto.getEndDate());
            data.put("totalLiters", dto.getTotalLiters());
            data.put("totalCost", dto.getTotalCost());
            data.put("avgUnitPrice", dto.getAvgUnitPrice());
            data.put("entryCount", dto.getEntryCount());
            
            List<Map<String, Object>> entryList = new ArrayList<>();
            for (FuelReportDTO.FuelEntry entry : dto.getEntries()) {
                Map<String, Object> entryMap = new HashMap<>();
                entryMap.put("date", entry.getDate());
                entryMap.put("station", entry.getStation());
                entryMap.put("liters", entry.getLiters());
                entryMap.put("unitPrice", entry.getUnitPrice());
                entryMap.put("total", entry.getTotal());
                entryMap.put("odometer", entry.getOdometer());
                entryList.add(entryMap);
            }
            data.put("entries", entryList);
            
            return data;
            
        } catch (Exception e) {
            log.error("Error getting fuel report data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get fuel report data", e);
        }
    }

    // ============================================================
    // HTML GENERATORS
    // ============================================================

    private String generateTripHTML(TripReportDTO trip) {
        // ... (keep your existing implementation)
        String statusColor = getStatusColor(trip.getStatus());
        String statusTextColor = getStatusTextColor(trip.getStatus());

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Trip Report - %s</title>
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { font-family: 'Segoe UI', Arial, sans-serif; background: #F7F7FC; padding: 20px; }
                .container { max-width: 1100px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; box-shadow: 0 4px 24px rgba(0,0,0,0.08); padding: 40px; }
                .header { text-align: center; border-bottom: 3px double #4F46E5; padding-bottom: 20px; margin-bottom: 30px; }
                .logo-container { display: flex; align-items: center; justify-content: center; gap: 14px; margin-bottom: 4px; }
                .logo-img { height: 50px; width: auto; }
                .logo-text { font-size: 28px; font-weight: 700; color: #4F46E5; }
                .logo-text span { color: #6366F1; }
                .subtitle { font-size: 14px; color: #6B7280; }
                .report-title { font-size: 20px; font-weight: 600; color: #111827; margin-top: 8px; }
                .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
                .section { background: #F9FAFB; border-radius: 12px; padding: 16px 20px; }
                .section-full { grid-column: 1 / -1; }
                .section-title { font-size: 13px; font-weight: 600; color: #4F46E5; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 12px; padding-bottom: 6px; border-bottom: 2px solid #E5E7EB; }
                .row { display: flex; padding: 6px 0; border-bottom: 1px solid #F3F4F6; }
                .row:last-child { border-bottom: none; }
                .label { width: 140px; font-size: 12px; color: #6B7280; flex-shrink: 0; }
                .value { font-size: 13px; font-weight: 500; color: #111827; }
                .status-badge { display: inline-block; padding: 2px 14px; border-radius: 20px; font-size: 11px; font-weight: 600; background-color: %s; color: %s; }
                .print-btn { display: inline-block; padding: 10px 24px; background: linear-gradient(135deg, #4F46E5 0%%, #6366F1 100%%); color: white; border: none; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; margin-top: 20px; }
                .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #E5E7EB; font-size: 11px; color: #9CA3AF; }
                @media print { body { background: white; padding: 0; } .container { box-shadow: none; padding: 20px; } .print-btn { display: none !important; } }
                @media (max-width: 768px) { .grid { grid-template-columns: 1fr; } .container { padding: 20px; } }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo-container">
                        <img src="%s" alt="SNL Trailers" class="logo-img" onerror="this.style.display='none'">
                        <div class="logo-text">🚛 <span>SNL</span> Trailers</div>
                    </div>
                    <div class="subtitle">Logistics &amp; Trucking Operations</div>
                    <div class="report-title">Trip Report</div>
                    <div style="font-size: 12px; color: #6B7280; margin-top: 4px;">
                        %s • Generated: %s
                    </div>
                </div>

                <div class="grid">
                    <div class="section">
                        <div class="section-title">Trip Details</div>
                        <div class="row"><span class="label">Trip Number</span><span class="value">%s</span></div>
                        <div class="row"><span class="label">Status</span><span class="value"><span class="status-badge" style="background-color:%s;color:%s;">%s</span></span></div>
                        <div class="row"><span class="label">Trip Type</span><span class="value">%s</span></div>
                        <div class="row"><span class="label">Reference</span><span class="value">%s</span></div>
                    </div>

                    <div class="section">
                        <div class="section-title">Customer</div>
                        <div class="row"><span class="label">Customer Name</span><span class="value">%s</span></div>
                        <div class="row"><span class="label">Customer Code</span><span class="value">%s</span></div>
                    </div>

                    <div class="section">
                        <div class="section-title">Vehicle &amp; Driver</div>
                        <div class="row"><span class="label">Vehicle</span><span class="value">%s</span></div>
                        <div class="row"><span class="label">Make / Model</span><span class="value">%s</span></div>
                        <div class="row"><span class="label">Driver</span><span class="value">%s</span></div>
                        <div class="row"><span class="label">License</span><span class="value">%s</span></div>
                    </div>

                    <div class="section">
                        <div class="section-title">Route &amp; Distance</div>
                        <div class="row"><span class="label">Origin</span><span class="value">%s</span></div>
                        <div class="row"><span class="label">Destination</span><span class="value">%s</span></div>
                        <div class="row"><span class="label">Planned Distance</span><span class="value">%.1f km</span></div>
                        <div class="row"><span class="label">Actual Distance</span><span class="value">%.1f km</span></div>
                    </div>

                    <div class="section section-full">
                        <div class="section-title">Timeline</div>
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 4px 24px;">
                            <div class="row"><span class="label">Planned Start</span><span class="value">%s</span></div>
                            <div class="row"><span class="label">Planned End</span><span class="value">%s</span></div>
                            <div class="row"><span class="label">Actual Start</span><span class="value">%s</span></div>
                            <div class="row"><span class="label">Actual End</span><span class="value">%s</span></div>
                            <div class="row"><span class="label">Odometer Start</span><span class="value">%.1f km</span></div>
                            <div class="row"><span class="label">Odometer End</span><span class="value">%.1f km</span></div>
                        </div>
                    </div>
                </div>

                <div style="text-align: center;" class="no-print">
                    <button class="print-btn" onclick="window.print()">🖨️ Print / Save as PDF</button>
                </div>

                <div class="footer">
                    <strong>SNL Trailers</strong> • Generated: %s • Confidential
                </div>
            </div>
        </body>
        </html>
        """.formatted(
            trip.getTripNumber(),
            statusColor, statusTextColor,
            LOGO_URL,
            trip.getTripNumber(), LocalDateTime.now().format(TIME_FORMATTER),
            trip.getTripNumber(),
            statusColor, statusTextColor, trip.getStatus() != null ? trip.getStatus() : "N/A",
            trip.getTripType() != null ? trip.getTripType() : "N/A",
            trip.getReferenceNumber() != null ? trip.getReferenceNumber() : "N/A",
            trip.getCustomerName() != null ? trip.getCustomerName() : "N/A",
            trip.getCustomerCode() != null ? trip.getCustomerCode() : "N/A",
            trip.getVehicleRegistration() != null ? trip.getVehicleRegistration() : "N/A",
            (trip.getVehicleMake() != null ? trip.getVehicleMake() : "") + " " + (trip.getVehicleModel() != null ? trip.getVehicleModel() : ""),
            trip.getDriverName() != null ? trip.getDriverName() : "N/A",
            trip.getDriverLicense() != null ? trip.getDriverLicense() : "N/A",
            trip.getOriginLocation() != null ? trip.getOriginLocation() : "N/A",
            trip.getDestinationLocation() != null ? trip.getDestinationLocation() : "N/A",
            trip.getPlannedDistanceKm() != null ? trip.getPlannedDistanceKm() : 0.0,
            trip.getActualDistanceKm() != null ? trip.getActualDistanceKm() : 0.0,
            trip.getPlannedStartDate() != null ? trip.getPlannedStartDate() : "N/A",
            trip.getPlannedEndDate() != null ? trip.getPlannedEndDate() : "N/A",
            trip.getActualStartDate() != null ? trip.getActualStartDate() : "N/A",
            trip.getActualEndDate() != null ? trip.getActualEndDate() : "N/A",
            trip.getActualStartOdometer() != null ? trip.getActualStartOdometer() : 0.0,
            trip.getActualEndOdometer() != null ? trip.getActualEndOdometer() : 0.0,
            LocalDateTime.now().format(TIME_FORMATTER)
        );
    }

    private String generateLoadHTML(LoadReportDTO load) {
    // ✅ Build trips table rows
    StringBuilder tripsHtml = new StringBuilder();
    
    if (load.getTrips() == null || load.getTrips().isEmpty()) {
        tripsHtml.append("""
            <tr>
                <td colspan="7" style="text-align:center; padding: 20px; color: #6B7280;">
                    No trips linked to this load
                </td>
            </tr>
            """);
    } else {
        for (LoadReportDTO.TripSummary trip : load.getTrips()) {
            tripsHtml.append("""
                <tr>
                    <td><strong>%s</strong></td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%.1f km</td>
                    <td><span class="status-badge" style="background-color:%s;color:%s;">%s</span></td>
                </tr>
                """.formatted(
                    trip.getTripNumber() != null ? trip.getTripNumber() : "N/A",
                    trip.getDriverName() != null ? trip.getDriverName() : "N/A",
                    trip.getVehicleRegistration() != null ? trip.getVehicleRegistration() : "N/A",
                    trip.getPlannedStartDate() != null ? trip.getPlannedStartDate() : "N/A",
                    trip.getPlannedEndDate() != null ? trip.getPlannedEndDate() : "N/A",
                    trip.getActualDistanceKm() != null ? trip.getActualDistanceKm() : 0.0,
                    getStatusColor(trip.getStatus()), 
                    getStatusTextColor(trip.getStatus()), 
                    trip.getStatus() != null ? trip.getStatus() : "N/A"
            ));
        }
    }

    // ✅ Build complete HTML with logo
    String statusColor = getStatusColor(load.getStatus());
    String statusTextColor = getStatusTextColor(load.getStatus());

    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <title>Load Report - %s</title>
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body { font-family: 'Segoe UI', Arial, sans-serif; background: #F7F7FC; padding: 20px; }
            .container { max-width: 1200px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; box-shadow: 0 4px 24px rgba(0,0,0,0.08); padding: 40px; }
            .header { text-align: center; border-bottom: 3px double #059669; padding-bottom: 20px; margin-bottom: 30px; }
            .logo-container { display: flex; align-items: center; justify-content: center; gap: 14px; margin-bottom: 4px; }
            .logo-img { height: 50px; width: auto; }
            .logo-text { font-size: 28px; font-weight: 700; color: #059669; }
            .logo-text span { color: #10B981; }
            .subtitle { font-size: 14px; color: #6B7280; }
            .report-title { font-size: 20px; font-weight: 600; color: #111827; margin-top: 8px; }
            .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
            .section { background: #F9FAFB; border-radius: 12px; padding: 16px 20px; }
            .section-full { grid-column: 1 / -1; }
            .section-title { font-size: 13px; font-weight: 600; color: #059669; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 12px; padding-bottom: 6px; border-bottom: 2px solid #E5E7EB; }
            .row { display: flex; padding: 6px 0; border-bottom: 1px solid #F3F4F6; }
            .row:last-child { border-bottom: none; }
            .label { width: 140px; font-size: 12px; color: #6B7280; flex-shrink: 0; }
            .value { font-size: 13px; font-weight: 500; color: #111827; }
            .status-badge { display: inline-block; padding: 2px 14px; border-radius: 20px; font-size: 11px; font-weight: 600; background-color: %s; color: %s; }
            .table-container { overflow-x: auto; margin-top: 16px; }
            table { width: 100%%; border-collapse: collapse; font-size: 12px; }
            th { background: #F3F4F6; padding: 10px 12px; text-align: left; font-weight: 600; color: #374151; border-bottom: 2px solid #E5E7EB; }
            td { padding: 8px 12px; border-bottom: 1px solid #F3F4F6; }
            .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #E5E7EB; font-size: 11px; color: #9CA3AF; }
            .print-btn { display: inline-block; padding: 10px 24px; background: linear-gradient(135deg, #059669 0%%, #10B981 100%%); color: white; border: none; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; margin-top: 20px; }
            @media print { body { background: white; padding: 0; } .container { box-shadow: none; padding: 20px; } .print-btn { display: none !important; } }
            @media (max-width: 768px) { .grid { grid-template-columns: 1fr; } .container { padding: 20px; } }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="header">
                <div class="logo-container">
                    <img src="%s" alt="SNL Trailers" class="logo-img" 
                         onerror="this.style.display='none'">
                    <div class="logo-text">📦 <span>SNL</span> Trailers</div>
                </div>
                <div class="subtitle">Logistics &amp; Trucking Operations</div>
                <div class="report-title">Load Report</div>
                <div style="font-size: 12px; color: #6B7280; margin-top: 4px;">
                    %s • Generated: %s
                </div>
            </div>

            <div class="grid">
                <div class="section">
                    <div class="section-title">Load Details</div>
                    <div class="row"><span class="label">Load Number</span><span class="value">%s</span></div>
                    <div class="row"><span class="label">Status</span><span class="value"><span class="status-badge" style="background-color:%s;color:%s;">%s</span></span></div>
                    <div class="row"><span class="label">Description</span><span class="value">%s</span></div>
                    <div class="row"><span class="label">Commodity</span><span class="value">%s</span></div>
                </div>

                <div class="section">
                    <div class="section-title">Measurements</div>
                    <div class="row"><span class="label">Weight</span><span class="value">%.1f kg</span></div>
                    <div class="row"><span class="label">Pallets</span><span class="value">%d</span></div>
                    <div class="row"><span class="label">Trips</span><span class="value">%d</span></div>
                    <div class="row"><span class="label">Customer</span><span class="value">%s</span></div>
                </div>

                <div class="section section-full">
                    <div class="section-title">Distance Summary</div>
                    <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 16px;">
                        <div style="text-align: center; padding: 12px; background: #DBEAFE; border-radius: 8px;">
                            <div style="font-size: 11px; color: #1E40AF;">Total Distance</div>
                            <div style="font-size: 20px; font-weight: 700; color: #1E40AF;">%.1f km</div>
                        </div>
                        <div style="text-align: center; padding: 12px; background: #D1FAE5; border-radius: 8px;">
                            <div style="font-size: 11px; color: #065F46;">From Depot</div>
                            <div style="font-size: 20px; font-weight: 700; color: #065F46;">%.1f km</div>
                        </div>
                        <div style="text-align: center; padding: 12px; background: #FEF3C7; border-radius: 8px;">
                            <div style="font-size: 11px; color: #92400E;">To Depot</div>
                            <div style="font-size: 20px; font-weight: 700; color: #92400E;">%.1f km</div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="section-title" style="margin-top: 24px;">Trips Linked to Load</div>
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>Trip Number</th>
                            <th>Driver</th>
                            <th>Vehicle</th>
                            <th>Planned Start</th>
                            <th>Planned End</th>
                            <th>Actual Distance</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        %s
                    </tbody>
                </table>
            </div>

            <div style="text-align: center;" class="no-print">
                <button class="print-btn" onclick="window.print()">🖨️ Print / Save as PDF</button>
            </div>

            <div class="footer">
                <strong>SNL Trailers</strong> • Generated: %s • Confidential
            </div>
        </div>
    </body>
    </html>
    """.formatted(
        load.getLoadNumber(),
        statusColor, statusTextColor,
        LOGO_URL,
        load.getLoadNumber(), LocalDateTime.now().format(TIME_FORMATTER),
        load.getLoadNumber(),
        statusColor, statusTextColor, load.getStatus() != null ? load.getStatus() : "N/A",
        load.getDescription() != null ? load.getDescription() : "N/A",
        load.getCommodityType() != null ? load.getCommodityType() : "N/A",
        load.getWeightKg() != null ? load.getWeightKg() : 0.0,
        load.getPalletCount() != null ? load.getPalletCount() : 0,
        load.getTripCount() != null ? load.getTripCount() : 0,
        load.getCustomerName() != null ? load.getCustomerName() : "N/A",
        load.getTotalDistanceKm() != null ? load.getTotalDistanceKm() : 0.0,
        load.getTotalFromDepotKm() != null ? load.getTotalFromDepotKm() : 0.0,
        load.getTotalToDepotKm() != null ? load.getTotalToDepotKm() : 0.0,
        tripsHtml.toString(),
        LocalDateTime.now().format(TIME_FORMATTER)
    );
}

    // ============================================================
    // FUEL HTML GENERATOR - ✅ UPDATED
    // ============================================================

    private String generateFuelHTML(FuelReportDTO report) {
        StringBuilder rows = new StringBuilder();
        
        if (report.getEntries().isEmpty()) {
            rows.append("""
                <tr>
                    <td colspan="6" style="text-align:center; padding: 30px; color: #6B7280;">
                        📭 No fuel entries found for the selected criteria
                    </td>
                </tr>
                """);
        } else {
            for (FuelReportDTO.FuelEntry entry : report.getEntries()) {
                rows.append("""
                    <tr>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%.1f</td>
                        <td>R %.2f</td>
                        <td>R %.2f</td>
                        <td>%.0f</td>
                    </tr>
                    """.formatted(
                        entry.getDate(),
                        entry.getStation(),
                        entry.getLiters(),
                        entry.getUnitPrice(),
                        entry.getTotal(),
                        entry.getOdometer()
                ));
            }
        }

        String vehicleDisplay = report.getVehicleRegistration() != null ? 
            report.getVehicleRegistration() : "All Vehicles";

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Fuel Report - %s</title>
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { font-family: 'Segoe UI', Arial, sans-serif; background: #F7F7FC; padding: 20px; }
                .container { max-width: 1200px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; box-shadow: 0 4px 24px rgba(0,0,0,0.08); padding: 40px; }
                .header { text-align: center; border-bottom: 3px double #D97706; padding-bottom: 20px; margin-bottom: 30px; }
                .logo-container { display: flex; align-items: center; justify-content: center; gap: 14px; margin-bottom: 4px; }
                .logo-img { height: 50px; width: auto; }
                .logo-text { font-size: 28px; font-weight: 700; color: #D97706; }
                .logo-text span { color: #F59E0B; }
                .report-title { font-size: 20px; font-weight: 600; color: #111827; margin-top: 8px; }
                .grid { display: grid; grid-template-columns: 1fr 1fr 1fr 1fr; gap: 16px; margin-bottom: 24px; }
                .stat-card { text-align: center; padding: 16px; background: #F9FAFB; border-radius: 12px; border: 1px solid #E5E7EB; }
                .stat-card .value { font-size: 24px; font-weight: 700; color: #111827; }
                .stat-card .label { font-size: 11px; color: #6B7280; text-transform: uppercase; letter-spacing: 0.3px; }
                .table-container { overflow-x: auto; }
                table { width: 100%%; border-collapse: collapse; font-size: 12px; }
                th { background: #F3F4F6; padding: 10px 12px; text-align: left; font-weight: 600; color: #374151; border-bottom: 2px solid #E5E7EB; }
                td { padding: 8px 12px; border-bottom: 1px solid #F3F4F6; }
                .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #E5E7EB; font-size: 11px; color: #9CA3AF; }
                .print-btn { display: inline-block; padding: 10px 24px; background: linear-gradient(135deg, #D97706 0%%, #F59E0B 100%%); color: white; border: none; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; margin-top: 20px; }
                @media print { body { background: white; padding: 0; } .container { box-shadow: none; padding: 20px; } .print-btn { display: none !important; } }
                @media (max-width: 768px) { .grid { grid-template-columns: 1fr 1fr; } .container { padding: 20px; } }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo-container">
                        <img src="%s" alt="SNL Trailers" class="logo-img" onerror="this.style.display='none'">
                        <div class="logo-text">⛽ <span>SNL</span> Trailers</div>
                    </div>
                    <div class="report-title">Fuel Consumption Report</div>
                    <div style="font-size: 12px; color: #6B7280; margin-top: 4px;">
                        Vehicle: %s • %s to %s • Generated: %s
                    </div>
                </div>

                <div class="grid">
                    <div class="stat-card">
                        <div class="value">%.1f L</div>
                        <div class="label">Total Liters</div>
                    </div>
                    <div class="stat-card">
                        <div class="value">R %.2f</div>
                        <div class="label">Total Cost</div>
                    </div>
                    <div class="stat-card">
                        <div class="value">R %.2f</div>
                        <div class="label">Avg Unit Price</div>
                    </div>
                    <div class="stat-card">
                        <div class="value">%d</div>
                        <div class="label">Transactions</div>
                    </div>
                </div>

                <div class="table-container">
                    <table>
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Station</th>
                                <th>Liters</th>
                                <th>Unit Price</th>
                                <th>Total</th>
                                <th>Odometer</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>

                <div style="text-align: center;" class="no-print">
                    <button class="print-btn" onclick="window.print()">🖨️ Print / Save as PDF</button>
                </div>

                <div class="footer">
                    <strong>SNL Trailers</strong> • Generated: %s • Confidential
                </div>
            </div>
        </body>
        </html>
        """.formatted(
            vehicleDisplay,
            LOGO_URL,
            vehicleDisplay,
            report.getStartDate() != null ? report.getStartDate() : "N/A",
            report.getEndDate() != null ? report.getEndDate() : "N/A",
            LocalDateTime.now().format(TIME_FORMATTER),
            report.getTotalLiters(),
            report.getTotalCost(),
            report.getAvgUnitPrice(),
            report.getEntryCount(),
            rows.toString(),
            LocalDateTime.now().format(TIME_FORMATTER)
        );
    }

    // ============================================================
    // ERROR HTML
    // ============================================================

    private String generateErrorHTML(String entity, String identifier, String error) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Error - %s</title>
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background: #FEF2F2; padding: 40px; }
                .container { max-width: 600px; margin: 40px auto; background: white; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); padding: 30px; border-left: 4px solid #DC2626; }
                h2 { color: #DC2626; font-size: 20px; margin-bottom: 8px; }
                .entity { font-size: 14px; color: #6B7280; margin-bottom: 4px; }
                .identifier { font-size: 13px; color: #374151; font-weight: 500; margin-bottom: 16px; }
                .error-detail { background: #F9FAFB; padding: 12px 16px; border-radius: 6px; font-family: monospace; font-size: 13px; color: #374151; margin: 12px 0; border: 1px solid #E5E7EB; word-break: break-all; }
                .suggestion { color: #6B7280; font-size: 13px; }
                .button { display: inline-block; margin-top: 16px; padding: 8px 20px; background: #4F46E5; color: white; border: none; border-radius: 6px; font-size: 14px; cursor: pointer; }
            </style>
        </head>
        <body>
            <div class="container">
                <h2>⚠️ Report Generation Error</h2>
                <div class="entity">Entity: %s</div>
                <div class="identifier">Identifier: %s</div>
                <div class="error-detail">%s</div>
                <p class="suggestion">Please try again or contact support.</p>
                <button class="button" onclick="window.history.back()">← Go Back</button>
            </div>
        </body>
        </html>
        """.formatted(entity, entity, identifier, error);
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private String getStatusColor(String status) {
        if (status == null) return "#F3F4F6";
        return switch (status.toUpperCase()) {
            case "COMPLETED", "FINALIZED" -> "#D1FAE5";
            case "IN_PROGRESS", "ACTIVE" -> "#DBEAFE";
            case "PLANNED", "ASSIGNED" -> "#FEF3C7";
            case "CANCELLED" -> "#FEE2E2";
            default -> "#F3F4F6";
        };
    }

    private String getStatusTextColor(String status) {
        if (status == null) return "#6B7280";
        return switch (status.toUpperCase()) {
            case "COMPLETED", "FINALIZED" -> "#065F46";
            case "IN_PROGRESS", "ACTIVE" -> "#1E40AF";
            case "PLANNED", "ASSIGNED" -> "#92400E";
            case "CANCELLED" -> "#991B1B";
            default -> "#6B7280";
        };
    }

    private String generateLoadHTMLWithLogo(LoadReportDTO load) {
        // This is a placeholder - use your existing load HTML with LOGO_URL
        // Make sure to add the logo-container div with LOGO_URL
        return "<html><body>Load Report with Logo</body></html>";
    }
}
