// src/main/java/com/pgsa/trailers/dto/report/TripReportDTO.java

package com.pgsa.trailers.dto.report;

import com.pgsa.trailers.entity.assets.Driver;      // ✅ Add this import
import com.pgsa.trailers.entity.assets.Vehicle;    // ✅ Add this import
import com.pgsa.trailers.entity.ops.Trip;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripReportDTO {
    private String tripNumber;
    private String tripType;
    private String status;
    private String approvalStatus;
    private String referenceNumber;
    
    private String customerName;
    private String customerCode;
    
    private String vehicleRegistration;
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleType;
    
    private String driverName;
    private String driverLicense;
    private String driverPhone;
    private String driverEmail;
    
    private String loadNumber;
    private String loadDescription;
    private String commodityType;
    
    private String originLocation;
    private String destinationLocation;
    private BigDecimal plannedDistanceKm;
    private BigDecimal actualDistanceKm;
    private BigDecimal actualStartOdometer;
    private BigDecimal actualEndOdometer;
    
    private String plannedStartDate;
    private String plannedEndDate;
    private String actualStartDate;
    private String actualEndDate;
    
    private BigDecimal costAmount;
    private BigDecimal revenueAmount;
    
    private String createdAt;
    private String updatedAt;

    public static TripReportDTO fromEntity(Trip trip) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
        
        // ✅ Driver fields - using correct field names from Driver entity
        String driverName = null;
        String driverLicense = null;
        String driverPhone = null;
        String driverEmail = null;
        
        if (trip.getDriver() != null) {
            Driver driver = trip.getDriver();
            // Full name
            String firstName = driver.getFirstName();
            String lastName = driver.getLastName();
            if (firstName != null || lastName != null) {
                driverName = (firstName != null ? firstName : "") + 
                             (lastName != null ? " " + lastName : "");
                driverName = driverName.trim();
                if (driverName.isEmpty()) driverName = null;
            }
            // ✅ Use correct field names from Driver entity
            driverLicense = driver.getLicenseNumber();
            driverPhone = driver.getPhoneNumber();
            driverEmail = driver.getEmail();
        }
        
        // ✅ Vehicle fields - using correct field names from Vehicle entity
        String vehicleMake = null;
        String vehicleModel = null;
        String vehicleRegistration = null;
        String vehicleType = null;
        
        if (trip.getVehicle() != null) {
            Vehicle vehicle = trip.getVehicle();
            vehicleMake = vehicle.getMake();
            vehicleModel = vehicle.getModel();
            vehicleRegistration = vehicle.getRegistrationNumber();
            vehicleType = vehicle.getVehicleType();
        }

        return TripReportDTO.builder()
                .tripNumber(trip.getTripNumber())
                .tripType(trip.getTripType())
                .status(trip.getStatus())
                .approvalStatus(trip.getApprovalStatus())
                .referenceNumber(trip.getReferenceNumber())
                .customerName(trip.getCustomer() != null ? trip.getCustomer().getName() : null)
                .customerCode(trip.getCustomer() != null ? trip.getCustomer().getCustomerCode() : null)
                .vehicleRegistration(vehicleRegistration)
                .vehicleMake(vehicleMake)
                .vehicleModel(vehicleModel)
                .vehicleType(vehicleType)
                .driverName(driverName)
                .driverLicense(driverLicense)
                .driverPhone(driverPhone)
                .driverEmail(driverEmail)
                .loadNumber(trip.getLoadNumber())
                .loadDescription(trip.getLoadDescription())
                .commodityType(trip.getCommodityType())
                .originLocation(trip.getOriginLocation())
                .destinationLocation(trip.getDestinationLocation())
                .plannedDistanceKm(trip.getPlannedDistanceKm())
                .actualDistanceKm(trip.getActualDistanceKm())
                .actualStartOdometer(trip.getActualStartOdometer())
                .actualEndOdometer(trip.getActualEndOdometer())
                .plannedStartDate(trip.getPlannedStartDate() != null ? 
                        trip.getPlannedStartDate().format(formatter) : null)
                .plannedEndDate(trip.getPlannedEndDate() != null ? 
                        trip.getPlannedEndDate().format(formatter) : null)
                .actualStartDate(trip.getActualStartDate() != null ? 
                        trip.getActualStartDate().format(formatter) : null)
                .actualEndDate(trip.getActualEndDate() != null ? 
                        trip.getActualEndDate().format(formatter) : null)
                .costAmount(trip.getCostAmount())
                .revenueAmount(trip.getRevenueAmount())
                .createdAt(trip.getCreatedAt() != null ? 
                        trip.getCreatedAt().format(formatter) : null)
                .updatedAt(trip.getUpdatedAt() != null ? 
                        trip.getUpdatedAt().format(formatter) : null)
                .build();
    }
}
