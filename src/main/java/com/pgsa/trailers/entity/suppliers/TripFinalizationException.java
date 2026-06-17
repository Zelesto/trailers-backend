package com.pgsa.trailers.exception;

public class TripFinalizationException extends BusinessException {
    
    public TripFinalizationException(String message) {
        super(message);
    }
    
    public TripFinalizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
