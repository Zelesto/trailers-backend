// src/main/java/com/pgsa/trailers/exception/InsufficientStockException.java
package com.pgsa.trailers.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
    
    public InsufficientStockException(String message, Throwable cause) {
        super(message, cause);
    }
}
