package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.dto.CreateTripRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateTripMapper {

    /**
     * Maps a CreateTripRequest to a Trip entity.
     * NOTE: Relationships (vehicle, driver, supervisor, customer, load) 
     * and tripNumber are set by the TripService.
     */
    public Trip toEntity(CreateTripRequest request) {
        if (request == null) {
            return null;
        }

        Trip trip = new Trip();

        // ======================== IDENTITY ========================
        trip.setTripType(request.getTripType());

        // ======================== WORKFLOW ========================
        // status is now a String, not an enum
        trip.setStatus(request.getStatus());
        trip.setApprovalStatus(request.getApprovalStatus());

        // ======================== PLANNING ========================
        trip.setPlannedStartDate(request.getPlannedStartDate());
        trip.setPlannedEndDate(request.getPlannedEndDate());
        trip.setPlannedDistanceKm(request.getPlannedDistanceKm());
        trip.setPlannedDurationHours(request.getPlannedDurationHours());
        trip.setEstimatedDurationHours(request.getEstimatedDurationHours());

        // ======================== CRANE USAGE ========================
        trip.setCraneUsed(request.getCraneUsed() != null ? request.getCraneUsed() : false);
    
        // Auto-detect from trip_type if not explicitly set
        if (!trip.getCraneUsed() && request.getTripType() != null) {
            String tripType = request.getTripType().toUpperCase();
            if (tripType.contains("CRANE") || tripType.contains("LIFT") || tripType.contains("HEAVY")) {
                trip.setCraneUsed(true);
            }
        }

        // ======================== COSTS ========================
        trip.setTollCost(request.getTollCost());
        trip.setOtherExpenses(request.getOtherExpenses());

        // ======================== CARGO ========================
        trip.setCommodityType(request.getCommodityType());
        trip.setCargoDescription(request.getCargoDescription());
        trip.setCargoWeight(request.getCargoWeight());
        trip.setCargoValue(request.getCargoValue());
        trip.setPalletCount(request.getPalletCount());
        trip.setContainerNumber(request.getContainerNumber());
        trip.setDistanceKm(request.getDistanceKm());
        trip.setFuelConsumedLiters(request.getFuelConsumedLiters());

        // ======================== ORIGIN LOCATION (REQUIRED) ========================
        if (request.getOriginLocation() != null && !request.getOriginLocation().isEmpty()) {
            trip.setOriginLocation(request.getOriginLocation());
        } else {
            // Build from components as fallback
            String builtOrigin = buildLocationFromComponents(
                request.getOriginStreetAddress(),
                request.getOriginCity(),
                request.getOriginZipCode(),
                request.getOriginProvince()
            );
            trip.setOriginLocation(builtOrigin.isEmpty() ? "Origin not specified" : builtOrigin);
            log.warn("Origin location was null, built from components: {}", builtOrigin);
        }

        // ======================== DESTINATION LOCATION (REQUIRED) ========================
        if (request.getDestinationLocation() != null && !request.getDestinationLocation().isEmpty()) {
            trip.setDestinationLocation(request.getDestinationLocation());
        } else {
            // Build from components as fallback
            String builtDestination = buildLocationFromComponents(
                request.getDestinationStreetAddress(),
                request.getDestinationCity(),
                request.getDestinationZipCode(),
                request.getDestinationProvince()
            );
            trip.setDestinationLocation(builtDestination.isEmpty() ? "Destination not specified" : builtDestination);
            log.warn("Destination location was null, built from components: {}", builtDestination);
        }

        // ======================== ORIGIN DETAILS ========================
        trip.setOriginStreetAddress(request.getOriginStreetAddress());
        trip.setOriginCity(request.getOriginCity());
        trip.setOriginZipCode(request.getOriginZipCode());
        trip.setOriginProvince(request.getOriginProvince());
        trip.setOriginLatitude(request.getOriginLatitude());
        trip.setOriginLongitude(request.getOriginLongitude());

        // ======================== DESTINATION DETAILS ========================
        trip.setDestinationStreetAddress(request.getDestinationStreetAddress());
        trip.setDestinationCity(request.getDestinationCity());
        trip.setDestinationZipCode(request.getDestinationZipCode());
        trip.setDestinationProvince(request.getDestinationProvince());
        trip.setDestinationLatitude(request.getDestinationLatitude());
        trip.setDestinationLongitude(request.getDestinationLongitude());

        // ======================== NOTES ========================
        trip.setNotes(request.getNotes());
        trip.setSpecialInstructions(request.getSpecialInstructions());
        trip.setDriverNotes(request.getDriverNotes());

        // ======================== REFERENCES ========================
        trip.setReferenceNumber(request.getReferenceNumber());
        trip.setPurchaseOrderNumber(request.getPurchaseOrderNumber());

        // ======================== OPERATIONS ========================
        trip.setIncidentsLogged(request.getIncidentsLogged() != null ? request.getIncidentsLogged() : 0);
        trip.setCancellationReason(request.getCancellationReason());

        // ======================== ROUTE DATA ========================
        trip.setGpsStartLocation(request.getGpsStartLocation());
        trip.setGpsEndLocation(request.getGpsEndLocation());
        trip.setRouteDetails(request.getRouteDetails());
        trip.setCheckpoints(request.getCheckpoints());

        // ======================== AUDIT ========================
        // FIXED: Convert to String for auditTrail
        Map<String, Object> auditMap = new HashMap<>();
        auditMap.put("action", "TRIP_CREATED");
        auditMap.put("timestamp", LocalDateTime.now());
        auditMap.put("createdBy", "system");
        auditMap.put("tripType", request.getTripType());
        // ✅ FIXED: status is now a String, so no need for .name()
        auditMap.put("status", request.getStatus() != null ? request.getStatus() : "DRAFT");
        
        trip.setAuditTrail(auditMap);

        // ======================== DEFAULT VALUES ========================
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
