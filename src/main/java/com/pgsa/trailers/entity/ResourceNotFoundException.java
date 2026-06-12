package com.pgsa.trailers.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {

    private String resourceName;
    private String fieldName;
    private Object fieldValue;

    public ResourceNotFoundException() {
        super();
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public ResourceNotFoundException(Class<?> resourceClass, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceClass.getSimpleName(), fieldName, fieldValue));
        this.resourceName = resourceClass.getSimpleName();
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public static ResourceNotFoundException forTrip(Long tripId) {
        return new ResourceNotFoundException("Trip", "id", tripId);
    }

    public static ResourceNotFoundException forDriver(Long driverId) {
        return new ResourceNotFoundException("Driver", "id", driverId);
    }

    public static ResourceNotFoundException forVehicle(Long vehicleId) {
        return new ResourceNotFoundException("Vehicle", "id", vehicleId);
    }

    public static ResourceNotFoundException forLoad(Long loadId) {
        return new ResourceNotFoundException("Load", "id", loadId);
    }

    public static ResourceNotFoundException forTripNumber(String tripNumber) {
        return new ResourceNotFoundException("Trip", "tripNumber", tripNumber);
    }
}
