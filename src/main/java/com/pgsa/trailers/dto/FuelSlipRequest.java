package com.pgsa.trailers.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FuelSlipRequest {
    private String slipNumber;
    private LocalDateTime transactionDate;
    private Long vehicleId;
    private String vehicleRegistration;
    private Long driverId;
    private String driverName;
    private String fuelType;
    private Double quantity;
    private Double unitPrice;
    private Double odometerReading; // Make sure this exists
    private String location;
    private String stationName;
    private String pumpNumber;
    private String receiptNumber;
    private String notes;
    private String paymentMethod;
    private Long tripId; // Make sure this exists
    private Long loadId;
    private Boolean finalized;

    // Optional fields
    private Long fuelSourceId;
    private Long accountStatementId;
}