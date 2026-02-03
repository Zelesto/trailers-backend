package com.pgsa.trailers.entity;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ========== CUSTOM EXCEPTIONS ==========

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setProperty("code", "RESOURCE_NOT_FOUND");
        problemDetail.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Business Rule Violation");
        problemDetail.setProperty("code", "BUSINESS_RULE_VIOLATION");
        problemDetail.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Validation Error");
        problemDetail.setProperty("code", "VALIDATION_ERROR");
        problemDetail.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    // ========== SPRING SECURITY EXCEPTIONS ==========

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "You don't have permission to access this resource"
        );
        problemDetail.setTitle("Access Denied");
        problemDetail.setProperty("code", "ACCESS_DENIED");
        problemDetail.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientAuthentication(InsufficientAuthenticationException ex) {
        log.warn("Authentication required: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Authentication required to access this resource"
        );
        problemDetail.setTitle("Authentication Required");
        problemDetail.setProperty("code", "AUTHENTICATION_REQUIRED");
        problemDetail.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials attempt: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
        );
        problemDetail.setTitle("Invalid Credentials");
        problemDetail.setProperty("code", "INVALID_CREDENTIALS");
        problemDetail.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    // ========== VALIDATION EXCEPTIONS ==========

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage()
                                : "Invalid value"
                ));

        log.warn("Request validation failed: {}", errors);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed for one or more fields"
        );
        problemDetail.setTitle("Validation Failed");
        problemDetail.setProperty("code", "VALIDATION_FAILED");
        problemDetail.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        problemDetail.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(fieldName, message);
        });

        log.warn("Constraint violation: {}", errors);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Constraint violation"
        );
        problemDetail.setTitle("Constraint Violation");
        problemDetail.setProperty("code", "CONSTRAINT_VIOLATION");
        problemDetail.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        problemDetail.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    // ========== DATA/DB EXCEPTIONS ==========

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: ", ex);

        String message = "Data integrity violation";
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            String causeMessage = ex.getCause().getMessage();
            if (causeMessage.contains("duplicate key")) {
                message = "Duplicate entry detected";
            } else if (causeMessage.contains("foreign key constraint")) {
                message = "Referenced data does not exist";
            } else if (causeMessage.contains("null value")) {
                message = "Required field cannot be null";
            }
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                message
        );
        problemDetail.setTitle("Data Integrity Violation");
        problemDetail.setProperty("code", "DATA_INTEGRITY_VIOLATION");
        problemDetail.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Requested entity not found"
        );
        problemDetail.setTitle("Entity Not Found");
        problemDetail.setProperty("code", "ENTITY_NOT_FOUND");
        problemDetail.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    // ========== REQUEST EXCEPTIONS ==========

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Invalid request body: {}", ex.getMessage());

        String message = "Invalid request body";
        if (ex.getCause() instanceof JsonParseException) {
            message = "Invalid JSON format";
        } else if (ex.getCause() instanceof JsonMappingException) {
            message = "Invalid JSON mapping";
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                message
        );
        problemDetail.setTitle("Invalid Request Body");
        problemDetail.setProperty("code", "INVALID_REQUEST_BODY");
        problemDetail.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                String.format("Missing required parameter: %s", ex.getParameterName())
        );
        problemDetail.setTitle("Missing Parameter");
        problemDetail.setProperty("code", "MISSING_PARAMETER");
        problemDetail.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch for parameter '{}': expected {}, got {}",
                ex.getName(), ex.getRequiredType(), ex.getValue());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                String.format("Invalid type for parameter '%s'", ex.getName())
        );
        problemDetail.setTitle("Type Mismatch");
        problemDetail.setProperty("code", "TYPE_MISMATCH");
        problemDetail.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    // ========== FILE UPLOAD EXCEPTIONS ==========

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("File upload size exceeded: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "File size exceeds maximum allowed limit"
        );
        problemDetail.setTitle("File Size Exceeded");
        problemDetail.setProperty("code", "FILE_SIZE_EXCEEDED");
        problemDetail.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problemDetail);
    }

    // ========== GENERIC EXCEPTION HANDLER ==========

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        log.error("Unhandled exception: ", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later."
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("code", "INTERNAL_SERVER_ERROR");
        problemDetail.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));

        // In development, add debug information
        if (isDevelopmentEnvironment()) {
            Map<String, String> debugInfo = new HashMap<>();
            debugInfo.put("exception", ex.getClass().getName());
            debugInfo.put("message", ex.getMessage());
            if (ex.getCause() != null) {
                debugInfo.put("cause", ex.getCause().getClass().getName());
            }
            problemDetail.setProperty("debug", debugInfo);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    // ========== HELPER METHODS ==========

    private boolean isDevelopmentEnvironment() {
        String env = System.getProperty("spring.profiles.active", "dev");
        return "dev".equals(env) || "development".equals(env);
    }
}

