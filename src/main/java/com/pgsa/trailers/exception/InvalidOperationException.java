// src/main/java/com/pgsa/trailers/exception/InvalidOperationException.java
package com.pgsa.trailers.exception;

public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }
    
    public InvalidOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
