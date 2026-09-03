// src/main/java/com/pgsa/trailers/exception/EntityNotFoundException.java
package com.pgsa.trailers.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
    
    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
