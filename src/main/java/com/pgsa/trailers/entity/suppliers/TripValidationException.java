package com.pgsa.trailers.entity.suppliers;

import lombok.Getter;
import java.math.BigDecimal;

@Getter
public class TripValidationException extends RuntimeException {

    private String errorCode;
    private Object[] args;

    public TripValidationException(String message) {
        super(message);
    }

    public TripValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TripValidationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public TripValidationException(String message, String errorCode, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    public static TripValidationException invalidStatusTransition(String from, String to) {
        return new TripValidationException(
            String.format("Invalid status transition from %s to %s", from, to),
            "INVALID_STATUS_TRANSITION",
            from, to
        );
    }

    public static TripValidationException missingRequiredField(String fieldName) {
        return new TripValidationException(
            String.format("Required field missing: %s", fieldName),
            "MISSING_REQUIRED_FIELD",
            fieldName
        );
    }

    public static TripValidationException invalidOdometerReading(BigDecimal start, BigDecimal end) {
        return new TripValidationException(
            String.format("Invalid odometer reading: start=%.2f, end=%.2f", start, end),
            "INVALID_ODOMETER_READING",
            start, end
        );
    }
}
