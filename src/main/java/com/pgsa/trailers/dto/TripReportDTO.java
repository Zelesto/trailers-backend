// src/main/java/com/pgsa/trailers/dto/report/TripReportDTO.java

package com.pgsa.trailers.dto.report;

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
    private String driverContact;
    
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
        
        return TripReportDTO.builder()
                .tripNumber(trip.getTripNumber())
                .tripType(trip.getTripType())
                .status(trip.getStatus())
                .approvalStatus(trip.getApprovalStatus())
                .referenceNumber(trip.getReferenceNumber())
                .customerName(trip.getCustomer() != null ? trip.getCustomer().getName() : null)
                .customerCode(trip.getCustomer() != null ? trip.getCustomer().getCustomerCode() : null)
                .vehicleRegistration(trip.getVehicle() != null ? trip.getVehicle().getRegistrationNumber() : null)
                .vehicleMake(trip.getVehicle() != null ? trip.getVehicle().getMake() : null)
                .vehicleModel(trip.getVehicle() != null ? trip.getVehicle().getModel() : null)
                .vehicleType(trip.getVehicle() != null ? trip.getVehicle().getVehicleType() : null)
                .driverName(trip.getDriver() != null ? 
                        trip.getDriver().getFirstName() + " " + trip.getDriver().getLastName() : null)
                .driverLicense(trip.getDriver() != null ? trip.getDriver().getLicenseNumber() : null)
                .driverContact(trip.getDriver() != null ? trip.getDriver().getContactNumber() : null)
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
