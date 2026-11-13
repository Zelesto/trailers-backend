package com.pgsa.trailers.entity;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import com.pgsa.trailers.entity.suppliers.TripValidationException;

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
        return build(HttpStatus.NOT_FOUND, "Resource Not Found", "RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Business Rule Violation",
                "BUSINESS_RULE_VIOLATION", ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Validation Error", "VALIDATION_ERROR", ex.getMessage());
    }

    // ========== ✅ NEW — DOMAIN STATE EXCEPTIONS ==========

    /**
     * Domain state errors, e.g.:
     *  - "Load already has an invoice: 42"
     *  - "Cannot generate invoice for Load X: billing subtotal is zero"
     *  - "Cannot delete a paid invoice"
     * Mapped to 422 so the frontend shows the real message.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY,
                "Unprocessable Entity", "ILLEGAL_STATE", ex.getMessage());
    }

    /**
     * Invalid arguments passed to services, e.g.:
     *  - "Load not found: LOAD-…"
     *  - "Customer not found for id: 42"
     * Mapped to 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST,
                "Invalid Argument", "INVALID_ARGUMENT", ex.getMessage());
    }

    // ========== SPRING SECURITY ==========

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Access Denied", "ACCESS_DENIED",
                "You don't have permission to access this resource");
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientAuthentication(InsufficientAuthenticationException ex) {
        log.warn("Authentication required: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Authentication Required",
                "AUTHENTICATION_REQUIRED", "Authentication required to access this resource");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials attempt: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Invalid Credentials",
                "INVALID_CREDENTIALS", "Invalid email or password");
    }

    // ========== VALIDATION ==========

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a));
        log.warn("Request validation failed: {}", errors);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed for one or more fields");
        pd.setTitle("Validation Failed");
        pd.setProperty("code", "VALIDATION_FAILED");
        pd.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        pd.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(v ->
                errors.put(v.getPropertyPath().toString(), v.getMessage()));
        log.warn("Constraint violation: {}", errors);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Constraint violation");
        pd.setTitle("Constraint Violation");
        pd.setProperty("code", "CONSTRAINT_VIOLATION");
        pd.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        pd.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of(
                "status", 405,
                "error", "Method Not Allowed",
                "message", "HTTP method " + ex.getMethod() + " is not supported for this endpoint",
                "supportedMethods", ex.getSupportedHttpMethods() != null
                        ? ex.getSupportedHttpMethods().toString() : "unknown"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFound(NoHandlerFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", "No endpoint for " + ex.getHttpMethod() + " " + ex.getRequestURL()));
    }

    // ========== DATA/DB ==========

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: ", ex);

        String message = "Data integrity violation";
        String cause = ex.getCause() != null ? ex.getCause().getMessage() : "";
        if (cause.contains("duplicate key")) {
            message = "Duplicate entry detected";
        } else if (cause.contains("violates foreign key constraint")) {
            message = cause.contains("update or delete")
                    ? "Cannot delete record because dependent records exist"
                    : "Referenced data does not exist";
        } else if (cause.contains("null value")) {
            // Extract the column name for a better message
            int idx = cause.indexOf("column \"");
            if (idx >= 0) {
                int end = cause.indexOf("\"", idx + 8);
                if (end > idx) {
                    String col = cause.substring(idx + 8, end);
                    message = "Required field cannot be null: " + col;
                } else {
                    message = "Required field cannot be null";
                }
            } else {
                message = "Required field cannot be null";
            }
        }

        return build(HttpStatus.CONFLICT, "Data Integrity Violation",
                "DATA_INTEGRITY_VIOLATION", message);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Entity Not Found",
                "ENTITY_NOT_FOUND", "Requested entity not found");
    }

    @ExceptionHandler(TripValidationException.class)
    public ResponseEntity<ProblemDetail> handleTripValidation(TripValidationException ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("Trip not found with ID")) {
            log.debug("⏳ Trip not found (possibly race condition): {}", message);
        } else {
            log.warn("Trip validation error: {}", message);
        }
        return build(HttpStatus.NOT_FOUND, "Resource Not Found", "RESOURCE_NOT_FOUND", message);
    }

    // ========== REQUEST ==========

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Invalid request body: {}", ex.getMessage());
        String message = "Invalid request body";
        Throwable cause = ex.getCause();
        if (cause instanceof JsonParseException)         message = "Invalid JSON format";
        else if (cause instanceof JsonMappingException)  message = "Invalid JSON mapping";
        return build(HttpStatus.BAD_REQUEST, "Invalid Request Body",
                "INVALID_REQUEST_BODY", message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Missing Parameter", "MISSING_PARAMETER",
                String.format("Missing required parameter: %s", ex.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch for parameter '{}': expected {}, got {}",
                ex.getName(), ex.getRequiredType(), ex.getValue());
        return build(HttpStatus.BAD_REQUEST, "Type Mismatch", "TYPE_MISMATCH",
                String.format("Invalid type for parameter '%s'", ex.getName()));
    }

    // ========== FILE UPLOAD ==========

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("File upload size exceeded: {}", ex.getMessage());
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "File Size Exceeded",
                "FILE_SIZE_EXCEEDED", "File size exceeds maximum allowed limit");
    }

    // ========== GENERIC FALLBACK ==========

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex) {
        log.error("Unhandled exception: ", ex);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
        pd.setTitle("Internal Server Error");
        pd.setProperty("code", "INTERNAL_SERVER_ERROR");
        pd.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));

        if (isDevelopmentEnvironment()) {
            Map<String, String> debug = new HashMap<>();
            debug.put("exception", ex.getClass().getName());
            debug.put("message", ex.getMessage() != null ? ex.getMessage() : "No message");
            if (ex.getCause() != null) {
                debug.put("cause", ex.getCause().getClass().getName());
                if (ex.getCause().getMessage() != null) {
                    debug.put("causeMessage", ex.getCause().getMessage());
                }
            }
            pd.setProperty("debug", debug);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

    // ========== HELPERS ==========

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String title,
                                                String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setProperty("code", code);
        pd.setProperty("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        return ResponseEntity.status(status).body(pd);
    }

    private boolean isDevelopmentEnvironment() {
        String env = System.getProperty("spring.profiles.active");
        if (env == null) env = System.getenv("SPRING_PROFILES_ACTIVE");
        return "dev".equals(env) || "development".equals(env) || env == null;
    }
}
