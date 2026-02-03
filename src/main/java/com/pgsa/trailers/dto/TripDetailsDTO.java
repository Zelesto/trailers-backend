package com.pgsa.trailers.dto;

import lombok.Data;

@Data
public class TripDetailsDTO {
    private Long id;
    private String tripNumber;
    private Long vehicleId;
    private String vehicleRegistration;
    private String vehicleMake;
    private String vehicleModel;
    private Long driverId;
    private String driverName;
    private String driverLicenseNumber;
}