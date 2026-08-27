// src/main/java/com/pgsa/trailers/service/ReportService.java

package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.report.FuelReportDTO;
import com.pgsa.trailers.dto.report.LoadReportDTO;
import com.pgsa.trailers.dto.report.TripReportDTO;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final TripRepository tripRepository;
    private final LoadRepository loadRepository;
    private final VehicleRepository vehicleRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ============================================================
    // TRIP REPORT
    // ============================================================

    @Transactional(readOnly = true)
    public String generateTripReportHTML(String tripNumber) {
        log.info("📊 Generating trip report for: {}", tripNumber);

        try {
            // ✅ Use the method that eagerly fetches all relationships
            Trip trip = tripRepository.findByTripNumberWithRelations(tripNumber)
                    .orElseThrow(() -> new RuntimeException("Trip not found: " + tripNumber));

            TripReportDTO reportDTO = TripReportDTO.fromEntity(trip);
            return generateTripHTML(reportDTO);
            
        } catch (Exception e) {
            log.error("Error generating trip report: {}", e.getMessage(), e);
            return generateErrorHTML(tripNumber, e.getMessage());
        }
    }

    // ============================================================
    // LOAD REPORT
    // ============================================================

    @Transactional(readOnly = true)
    public String generateLoadReportHTML(String loadNumber) {
        log.info("📊 Generating load report for: {}", loadNumber);

        try {
            // ✅ Try to find load by load number
            Optional<Load> loadOpt = loadRepository.findByLoadNumber(loadNumber);
            
            if (loadOpt.isEmpty()) {
                throw new RuntimeException("Load not found: " + loadNumber);
            }
            
            Load load = loadOpt.get();
            
            // Get trips for this load - handle null loadId
            List<Trip> trips = new ArrayList<>();
            if (load.getLoadNumber() != null) {
                trips = tripRepository.findByLoadId(load.getLoadNumber());
            }
            
            LoadReportDTO reportDTO = LoadReportDTO.fromEntity(load, trips);
            return generateLoadHTML(reportDTO);
            
        } catch (Exception e) {
            log.error("Error generating load report: {}", e.getMessage(), e);
            return generateErrorHTML(loadNumber, e.getMessage());
        }
    }

    // ============================================================
    // FUEL REPORT
    // ============================================================

    @Transactional(readOnly = true)
    public String generateFuelReportHTML(Long vehicleId, String startDate, String endDate) {
        log.info("📊 Generating fuel report for vehicle: {} from {} to {}", vehicleId, startDate, endDate);

        try {
            // For now, return sample data
            FuelReportDTO report = FuelReportDTO.createSample(vehicleId);
            return generateFuelHTML(report);
        } catch (Exception e) {
            log.error("Error generating fuel report: {}", e.getMessage(), e);
            return generateErrorHTML("Fuel Report", e.getMessage());
        }
    }

    // ============================================================
    // HTML GENERATORS
    // ============================================================

    private String generateTripHTML(TripReportDTO trip) {
        // ... existing trip HTML generator ...
        return generateTripHTMLContent(trip);
    }

    private String generateLoadHTML(LoadReportDTO load) {
        // ... existing load HTML generator ...
        return generateLoadHTMLContent(load);
    }

    private String generateFuelHTML(FuelReportDTO report) {
        // ... existing fuel HTML generator ...
        return generateFuelHTMLContent(report);
    }

    private String generateErrorHTML(String entity, String error) {
        return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"><title>Error</title></head>
        <body style="font-family:Arial;text-align:center;padding:40px;">
            <h2 style="color:#EF4444;">⚠️ Report Generation Error</h2>
            <p><strong>Entity:</strong> %s</p>
            <p style="color:#6B7280;">%s</p>
            <p style="font-size:12px;color:#9CA3AF;margin-top:20px;">Please try again or contact support.</p>
        </body>
        </html>
        """.formatted(entity, error);
    }

    // ============================================================
    // TRIP HTML CONTENT
    // ============================================================

    private String generateTripHTMLContent(TripReportDTO trip) {
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
                .logo { font-size: 28px; font-weight: 700; color: #4F46E5; }
                .logo span { color: #6366F1; }
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
                .print-btn { display: inline-block; padding: 10px 24px; background: linear-gradient(135deg, #4F46E5 0%, #6366F1 100%); color: white; border: none; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; margin-top: 20px; }
                @media print { body { background: white; padding: 0; } .container { box-shadow: none; padding: 20px; } .print-btn { display: none !important; } }
                @media (max-width: 768px) { .grid { grid-template-columns: 1fr; } .container { padding: 20px; } }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo">🚛 <span>PGS</span> Trailers</div>
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
                    <strong>PGS Trailers</strong> • Generated: %s • Confidential
                </div>
            </div>
        </body>
        </html>
        """.formatted(
            trip.getTripNumber(),
            statusColor, statusTextColor,
            trip.getTripNumber(), new Date().toString(),
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
            new Date().toString()
        );
    }

    // ============================================================
    // LOAD HTML CONTENT
    // ============================================================

    private String generateLoadHTMLContent(LoadReportDTO load) {
        StringBuilder tripsHtml = new StringBuilder();
        for (LoadReportDTO.TripSummary trip : load.getTrips()) {
            tripsHtml.append("""
                <tr>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%.1f km</td>
                    <td><span class="status-badge" style="background-color:%s;color:%s;">%s</span></td>
                </tr>
                """.formatted(
                    trip.getTripNumber(),
                    trip.getDriverName(),
                    trip.getVehicleRegistration(),
                    trip.getPlannedStartDate(),
                    trip.getPlannedEndDate(),
                    trip.getActualDistanceKm(),
                    getStatusColor(trip.getStatus()), getStatusTextColor(trip.getStatus()), trip.getStatus()
            ));
        }

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
                .logo { font-size: 28px; font-weight: 700; color: #059669; }
                .logo span { color: #10B981; }
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
                table { width: 100%; border-collapse: collapse; font-size: 12px; }
                th { background: #F3F4F6; padding: 10px 12px; text-align: left; font-weight: 600; color: #374151; border-bottom: 2px solid #E5E7EB; }
                td { padding: 8px 12px; border-bottom: 1px solid #F3F4F6; }
                .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #E5E7EB; font-size: 11px; color: #9CA3AF; }
                .print-btn { display: inline-block; padding: 10px 24px; background: linear-gradient(135deg, #059669 0%, #10B981 100%); color: white; border: none; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; margin-top: 20px; }
                .badge { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; }
                .badge-success { background: #D1FAE5; color: #065F46; }
                .badge-warning { background: #FEF3C7; color: #92400E; }
                @media print { body { background: white; padding: 0; } .container { box-shadow: none; padding: 20px; } .print-btn { display: none !important; } }
                @media (max-width: 768px) { .grid { grid-template-columns: 1fr; } .container { padding: 20px; } }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo">📦 <span>PGS</span> Trailers</div>
                    <div class="subtitle">Logistics &amp; Trucking Operations</div>
                    <div class="report-title">Load Report</div>
                    <div style="font-size: 12px; color: #6B7280; margin-top: 4px;">
                        %s • %s • Generated: %s
                    </div>
                </div>

                <div class="grid">
                    <div class="section">
                        <div class="section-title">Load Details</div>
                        <div class="row"><span class="label">Load Number</span><span class="value">%s</span></div>
                        <div class="row"><span class="label">Status</span><span class="value"><span class="badge badge-success">%s</span></span></div>
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
                    <strong>PGS Trailers</strong> • Generated: %s • Confidential
                </div>
            </div>
        </body>
        </html>
        """.formatted(
            load.getLoadNumber(),
            "#D1FAE5", "#065F46",
            load.getLoadNumber(), load.getStatus(), new Date().toString(),
            load.getLoadNumber(),
            load.getStatus(),
            load.getDescription() != null ? load.getDescription() : "N/A",
            load.getCommodityType() != null ? load.getCommodityType() : "N/A",
            load.getWeightKg() != null ? load.getWeightKg() : 0.0,
            load.getPalletCount() != null ? load.getPalletCount() : 0,
            load.getTripCount(),
            load.getCustomerName() != null ? load.getCustomerName() : "N/A",
            load.getTotalDistanceKm() != null ? load.getTotalDistanceKm() : 0.0,
            load.getTotalFromDepotKm() != null ? load.getTotalFromDepotKm() : 0.0,
            load.getTotalToDepotKm() != null ? load.getTotalToDepotKm() : 0.0,
            tripsHtml.toString(),
            new Date().toString()
        );
    }

    // ============================================================
    // FUEL HTML CONTENT
    // ============================================================

    private String generateFuelHTMLContent(FuelReportDTO report) {
        StringBuilder rows = new StringBuilder();
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

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Fuel Report</title>
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { font-family: 'Segoe UI', Arial, sans-serif; background: #F7F7FC; padding: 20px; }
                .container { max-width: 1200px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; box-shadow: 0 4px 24px rgba(0,0,0,0.08); padding: 40px; }
                .header { text-align: center; border-bottom: 3px double #D97706; padding-bottom: 20px; margin-bottom: 30px; }
                .logo { font-size: 28px; font-weight: 700; color: #D97706; }
                .logo span { color: #F59E0B; }
                .report-title { font-size: 20px; font-weight: 600; color: #111827; margin-top: 8px; }
                .grid { display: grid; grid-template-columns: 1fr 1fr 1fr 1fr; gap: 16px; margin-bottom: 24px; }
                .stat-card { text-align: center; padding: 16px; background: #F9FAFB; border-radius: 12px; border: 1px solid #E5E7EB; }
                .stat-card .value { font-size: 24px; font-weight: 700; color: #111827; }
                .stat-card .label { font-size: 11px; color: #6B7280; text-transform: uppercase; }
                .table-container { overflow-x: auto; }
                table { width: 100%; border-collapse: collapse; font-size: 12px; }
                th { background: #F3F4F6; padding: 10px 12px; text-align: left; font-weight: 600; color: #374151; border-bottom: 2px solid #E5E7EB; }
                td { padding: 8px 12px; border-bottom: 1px solid #F3F4F6; }
                .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #E5E7EB; font-size: 11px; color: #9CA3AF; }
                .print-btn { display: inline-block; padding: 10px 24px; background: linear-gradient(135deg, #D97706 0%, #F59E0B 100%); color: white; border: none; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; margin-top: 20px; }
                @media print { body { background: white; padding: 0; } .container { box-shadow: none; padding: 20px; } .print-btn { display: none !important; } }
                @media (max-width: 768px) { .grid { grid-template-columns: 1fr 1fr; } .container { padding: 20px; } }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo">⛽ <span>PGS</span> Trailers</div>
                    <div class="report-title">Fuel Consumption Report</div>
                    <div style="font-size: 12px; color: #6B7280; margin-top: 4px;">
                        Vehicle: %s • %s to %s • Generated: %s
                    </div>
                </div>

                <div class="grid">
                    <div class="stat-card"><div class="value">%.1f L</div><div class="label">Total Liters</div></div>
                    <div class="stat-card"><div class="value">R %.2f</div><div class="label">Total Cost</div></div>
                    <div class="stat-card"><div class="value">R %.2f</div><div class="label">Avg Unit Price</div></div>
                    <div class="stat-card"><div class="value">%d</div><div class="label">Transactions</div></div>
                </div>

                <div class="table-container">
                    <table>
                        <thead>
                            <tr><th>Date</th><th>Station</th><th>Liters</th><th>Unit Price</th><th>Total</th><th>Odometer</th></tr>
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
                    <strong>PGS Trailers</strong> • Generated: %s • Confidential
                </div>
            </div>
        </body>
        </html>
        """.formatted(
            report.getVehicleRegistration() != null ? report.getVehicleRegistration() : "All Vehicles",
            report.getStartDate() != null ? report.getStartDate() : "N/A",
            report.getEndDate() != null ? report.getEndDate() : "N/A",
            new Date().toString(),
            report.getTotalLiters(),
            report.getTotalCost(),
            report.getAvgUnitPrice(),
            report.getEntryCount(),
            rows.toString(),
            new Date().toString()
        );
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
}
