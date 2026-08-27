// src/main/java/com/pgsa/trailers/service/ReportService.java

package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final TripRepository tripRepository;

    /**
     * Generate trip report in specified format
     * Currently supports HTML format with print/PDF capability
     */
    public byte[] generateTripReport(String tripNumber, String format) throws Exception {
        log.info("📊 Generating report for trip: {} (format: {})", tripNumber, format);

        Trip trip = tripRepository.findByTripNumber(tripNumber)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + tripNumber));

        // Generate HTML report
        String htmlContent = generateTripHTML(trip);
        
        // For HTML format, return directly
        if ("html".equalsIgnoreCase(format)) {
            return htmlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        
        // For other formats, still return HTML (browser will handle print to PDF)
        log.warn("Format '{}' requested, returning HTML (browser print to PDF available)", format);
        return htmlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String generateTripHTML(Trip trip) {
        String driverName = trip.getDriver() != null ?
                (trip.getDriver().getFirstName() + " " + trip.getDriver().getLastName()) : "N/A";
        String vehicleReg = trip.getVehicle() != null ?
                trip.getVehicle().getRegistrationNumber() : "N/A";
        String vehicleType = trip.getVehicle() != null && trip.getVehicle().getVehicleType() != null ?
                trip.getVehicle().getVehicleType() : "N/A";
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        
        String statusColor = getStatusColor(trip.getStatus());
        String statusTextColor = getStatusTextColor(trip.getStatus());
        String statusDisplay = trip.getStatus() != null ? trip.getStatus() : "N/A";

        // ✅ Fix: Handle BigDecimal subtraction properly
        BigDecimal startOdometer = trip.getActualStartOdometer() != null ? trip.getActualStartOdometer() : BigDecimal.ZERO;
        BigDecimal endOdometer = trip.getActualEndOdometer() != null ? trip.getActualEndOdometer() : BigDecimal.ZERO;
        BigDecimal totalOdometer = endOdometer.subtract(startOdometer);

        // ✅ Fix: Get distance value
        BigDecimal distance = trip.getActualDistanceKm() != null ? 
                trip.getActualDistanceKm() : 
                (trip.getPlannedDistanceKm() != null ? trip.getPlannedDistanceKm() : BigDecimal.ZERO);

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
                .logo { font-size: 28px; font-weight: 700; color: #4F46E5; letter-spacing: -0.5px; }
                .logo span { color: #6366F1; }
                .subtitle { font-size: 14px; color: #6B7280; margin-top: 4px; }
                .report-title { font-size: 20px; font-weight: 600; color: #111827; margin-top: 8px; }
                .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
                .section { background: #F9FAFB; border-radius: 12px; padding: 16px 20px; }
                .section-full { grid-column: 1 / -1; }
                .section-title { font-size: 13px; font-weight: 600; color: #4F46E5; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 12px; padding-bottom: 6px; border-bottom: 2px solid #E5E7EB; }
                .row { display: flex; padding: 6px 0; border-bottom: 1px solid #F3F4F6; }
                .row:last-child { border-bottom: none; }
                .label { width: 130px; font-size: 12px; color: #6B7280; flex-shrink: 0; }
                .value { font-size: 13px; font-weight: 500; color: #111827; }
                .status-badge { 
                    display: inline-block; 
                    padding: 2px 14px; 
                    border-radius: 20px; 
                    font-size: 11px; 
                    font-weight: 600;
                    background-color: %s;
                    color: %s;
                }
                .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #E5E7EB; font-size: 11px; color: #9CA3AF; }
                .footer strong { color: #4F46E5; }
                .print-btn { 
                    display: inline-block; 
                    padding: 10px 24px; 
                    background: linear-gradient(135deg, #4F46E5 0%, #6366F1 100%);
                    color: white; 
                    border: none; 
                    border-radius: 10px; 
                    font-size: 14px; 
                    font-weight: 600;
                    cursor: pointer;
                    margin-top: 20px;
                    transition: transform 0.2s;
                }
                .print-btn:hover { transform: scale(1.02); }
                @media print {
                    body { background: white; padding: 0; }
                    .container { box-shadow: none; padding: 20px; }
                    .section { background: #F9FAFB; }
                    .print-btn { display: none !important; }
                    .no-print { display: none !important; }
                }
                @media (max-width: 768px) {
                    .grid { grid-template-columns: 1fr; }
                    .container { padding: 20px; }
                }
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
                        <div class="row"><span class="label">Customer</span><span class="value">%s</span></div>
                        <div class="row"><span class="label">Reference</span><span class="value">%s</span></div>
                    </div>

                    <div class="section">
                        <div class="section-title">Route Information</div>
                        <div class="row"><span class="label">Origin</span><span class="value">%s</span></div>
                        <div class="row"><span class="label">Destination</span><span class="value">%s</span></div>
                        <div class="row"><span class="label">Distance</span><span class="value">%.2f km</span></div>
                    </div>

                    <div class="section">
                        <div class="section-title">Driver &amp; Vehicle</div>
                        <div class="row"><span class="label">Driver</span><span class="value">%s</span></div>
                        <div class="row"><span class="label">Vehicle</span><span class="value">%s</span></div>
                        <div class="row"><span class="label">Type</span><span class="value">%s</span></div>
                    </div>

                    <div class="section">
                        <div class="section-title">Odometer</div>
                        <div class="row"><span class="label">Start</span><span class="value">%.2f km</span></div>
                        <div class="row"><span class="label">End</span><span class="value">%.2f km</span></div>
                        <div class="row"><span class="label">Total</span><span class="value">%.2f km</span></div>
                    </div>

                    <div class="section section-full">
                        <div class="section-title">Timeline</div>
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 4px 24px;">
                            <div class="row"><span class="label">Planned Start</span><span class="value">%s</span></div>
                            <div class="row"><span class="label">Actual Start</span><span class="value">%s</span></div>
                            <div class="row"><span class="label">Planned End</span><span class="value">%s</span></div>
                            <div class="row"><span class="label">Actual End</span><span class="value">%s</span></div>
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
            trip.getTripNumber(),
            new Date().toString(),
            trip.getTripNumber(),
            statusColor, statusTextColor, statusDisplay,
            trip.getCustomer() != null ? trip.getCustomer().getName() : "N/A",
            trip.getReferenceNumber() != null ? trip.getReferenceNumber() : "N/A",
            trip.getOriginLocation() != null ? trip.getOriginLocation() : "N/A",
            trip.getDestinationLocation() != null ? trip.getDestinationLocation() : "N/A",
            distance,
            driverName,
            vehicleReg,
            vehicleType,
            startOdometer,
            endOdometer,
            totalOdometer,
            trip.getPlannedStartDate() != null ? trip.getPlannedStartDate().format(formatter) : "N/A",
            trip.getActualStartDate() != null ? trip.getActualStartDate().format(formatter) : "N/A",
            trip.getPlannedEndDate() != null ? trip.getPlannedEndDate().format(formatter) : "N/A",
            trip.getActualEndDate() != null ? trip.getActualEndDate().format(formatter) : "N/A",
            new Date().toString()
        );
    }

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

    /**
     * Check if BIRT is available (for backward compatibility)
     */
    public boolean isBirtAvailable() {
        return false; // Using pure HTML reporting now
    }
}
