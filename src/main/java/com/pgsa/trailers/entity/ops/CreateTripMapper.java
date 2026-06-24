package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.CreateTripRequest;
import com.pgsa.trailers.enums.TripStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateTripMapper {

    public Trip toEntity(CreateTripRequest request) {
        if (request == null) {
            return null;
        }

        Trip trip = new Trip();

        // NOTE: tripNumber is set by the service using TripNumberGenerator
        // NOTE: vehicle, driver, supervisor, customer, load are set by the service

        /* ========================
           IDENTITY
           ======================== */
        trip.setTripType(request.getTripType());

        /* ========================
           WORKFLOW
           ======================== */
        trip.setStatus(request.getStatus() != null ? request.getStatus() : TripStatus.DRAFT);
        trip.setApprovalStatus(request.getApprovalStatus());

        /* ========================
           PLANNING
           ======================== */
        trip.setPlannedStartDate(request.getPlannedStartDate());
        trip.setPlannedEndDate(request.getPlannedEndDate());
        
        if (request.getPlannedDistanceKm() != null) {
            trip.setPlannedDistanceKm(BigDecimal.valueOf(request.getPlannedDistanceKm()));
        }
        
        if (request.getPlannedDurationHours() != null) {
            trip.setPlannedDurationHours(BigDecimal.valueOf(request.getPlannedDurationHours()));
        }
        
        if (request.getEstimatedDurationHours() != null) {
            trip.setEstimatedDurationHours(BigDecimal.valueOf(request.getEstimatedDurationHours()));
        }

        /* ========================
           COSTS
           ======================== */
        if (request.getTollCost() != null) {
            trip.setTollCost(BigDecimal.valueOf(request.getTollCost()));
        }
        if (request.getOtherExpenses() != null) {
            trip.setOtherExpenses(BigDecimal.valueOf(request.getOtherExpenses()));
        }

        /* ========================
           CARGO
           ======================== */
        trip.setCommodityType(request.getCommodityType());
        trip.setCargoDescription(request.getCargoDescription());
        
        if (request.getCargoWeight() != null) {
            trip.setCargoWeight(BigDecimal.valueOf(request.getCargoWeight()));
        }
        
        if (request.getCargoValue() != null) {
            trip.setCargoValue(BigDecimal.valueOf(request.getCargoValue()));
        }
        
        trip.setPalletCount(request.getPalletCount());
        trip.setContainerNumber(request.getContainerNumber());

        if (request.getDistanceKm() != null) {
            trip.setDistanceKm(BigDecimal.valueOf(request.getDistanceKm()));
        }
        
        if (request.getFuelConsumedLiters() != null) {
            trip.setFuelConsumedLiters(BigDecimal.valueOf(request.getFuelConsumedLiters()));
        }

        /* ========================
           ORIGIN LOCATION (REQUIRED)
           ======================== */
        // Origin location is required (nullable = false)
        if (request.getOriginLocation() != null && !request.getOriginLocation().isEmpty()) {
            trip.setOriginLocation(request.getOriginLocation());
        } else {
            // Build from components
            String builtOrigin = buildLocationFromComponents(
                request.getOriginStreetAddress(),
                request.getOriginCity(),
                request.getOriginZipCode(),
                request.getOriginProvince()
            );
            // If still empty, set a default to avoid NOT NULL constraint violation
            trip.setOriginLocation(builtOrigin.isEmpty() ? "Origin not specified" : builtOrigin);
        }

        /* ========================
           DESTINATION LOCATION (REQUIRED)
           ======================== */
        // Destination location is required (nullable = false)
        if (request.getDestinationLocation() != null && !request.getDestinationLocation().isEmpty()) {
            trip.setDestinationLocation(request.getDestinationLocation());
        } else {
            // Build from components
            String builtDestination = buildLocationFromComponents(
                request.getDestinationStreetAddress(),
                request.getDestinationCity(),
                request.getDestinationZipCode(),
                request.getDestinationProvince()
            );
            // If still empty, set a default to avoid NOT NULL constraint violation
            trip.setDestinationLocation(builtDestination.isEmpty() ? "Destination not specified" : builtDestination);
        }

        /* ========================
           ORIGIN DETAILS
           ======================== */
        trip.setOriginStreetAddress(request.getOriginStreetAddress());
        trip.setOriginCity(request.getOriginCity());
        trip.setOriginZipCode(request.getOriginZipCode());
        trip.setOriginProvince(request.getOriginProvince());
        trip.setOriginLatitude(request.getOriginLatitude());
        trip.setOriginLongitude(request.getOriginLongitude());

        /* ========================
           DESTINATION DETAILS
           ======================== */
        trip.setDestinationStreetAddress(request.getDestinationStreetAddress());
        trip.setDestinationCity(request.getDestinationCity());
        trip.setDestinationZipCode(request.getDestinationZipCode());
        trip.setDestinationProvince(request.getDestinationProvince());
        trip.setDestinationLatitude(request.getDestinationLatitude());
        trip.setDestinationLongitude(request.getDestinationLongitude());

        /* ========================
           NOTES
           ======================== */
        trip.setNotes(request.getNotes());
        trip.setSpecialInstructions(request.getSpecialInstructions());
        trip.setDriverNotes(request.getDriverNotes());

        /* ========================
           REFERENCES
           ======================== */
        trip.setReferenceNumber(request.getReferenceNumber());
        trip.setPurchaseOrderNumber(request.getPurchaseOrderNumber());

        /* ========================
           OPERATIONS
           ======================== */
        trip.setIncidentsLogged(request.getIncidentsLogged() != null ? request.getIncidentsLogged() : 0);
        trip.setCancellationReason(request.getCancellationReason());

        /* ========================
           ROUTE DATA
           ======================== */
        trip.setGpsStartLocation(request.getGpsStartLocation());
        trip.setGpsEndLocation(request.getGpsEndLocation());
        trip.setRouteDetails(request.getRouteDetails());
        trip.setCheckpoints(request.getCheckpoints());

        /* ========================
           AUDIT
           ======================== */
        trip.setAuditTrail(request.getAuditTrail());

        /* ========================
           DEFAULT VALUES
           ======================== */
        trip.setLastStatusUpdate(LocalDateTime.now());
        trip.setIsActive(true);

        log.debug("Mapped CreateTripRequest to Trip entity");
        
        return trip;
    }

    /**
     * Build a location string from components
     */
    private String buildLocationFromComponents(String street, String city, String zip, String province) {
        StringBuilder location = new StringBuilder();
        
        if (street != null && !street.isBlank()) {
            location.append(street);
        }
        
        if (city != null && !city.isBlank()) {
            if (!location.isEmpty()) location.append(", ");
            location.append(city);
        }
        
        if (zip != null && !zip.isBlank()) {
            if (!location.isEmpty()) location.append(" ");
            location.append(zip);
        }
        
        if (province != null && !province.isBlank()) {
            if (!location.isEmpty()) location.append(", ");
            location.append(province);
        }
        
        return location.toString();
    }
}
