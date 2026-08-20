package com.pgsa.trailers.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.ops.TripResponseMapper;
import com.pgsa.trailers.entity.security.AppUser;
import com.pgsa.trailers.repository.AppUserRepository;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.repository.VehicleRepository;
import com.pgsa.trailers.service.SequenceService;
import com.pgsa.trailers.service.security.CustomUserDetailsService;
import com.pgsa.trailers.service.security.JwtService;
import com.pgsa.trailers.service.util.TripNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AppUserRepository appUserRepository;
    private final TripNumberGenerator tripNumberGenerator;
    private final SequenceService sequenceService;
    private final JdbcTemplate jdbcTemplate;
    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final TripResponseMapper tripResponseMapper;

    // ============================================================
    // CONSTANTS FOR STATUS VALUES (from enum_master table)
    // ============================================================
    public static final String STATUS_PLANNED = "PLANNED";

    // ======================== HEALTH ========================

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(createResponse(true, Map.of(
                "status", "UP",
                "service", "Test Controller",
                "timestamp", System.currentTimeMillis()
        )));
    }

    // ======================== TEST GENERATE ========================

    @GetMapping("/test-generate")
    public ResponseEntity<Map<String, Object>> testGenerate() {
        Map<String, Object> response = new HashMap<>();
        try {
            // Test 1: Direct JdbcTemplate
            String year = String.valueOf(java.time.Year.now().getValue());
            String prefix = "TRP-" + year + "-";
            Long nextNumber = jdbcTemplate.queryForObject(
                "INSERT INTO sequence (table_name, year, next_number, created_at, updated_at) " +
                "VALUES ('trip', ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (table_name, year) DO UPDATE SET next_number = sequence.next_number + 1 " +
                "RETURNING next_number - 1",
                new Object[]{year},
                Long.class
            );
            String directNumber = prefix + String.format("%03d", nextNumber);
            
            // Test 2: TripNumberGenerator
            String generatorNumber = tripNumberGenerator.generate();
            
            response.put("success", true);
            response.put("directJdbc", directNumber);
            response.put("generator", generatorNumber);
            response.put("sequenceYear", year);
            response.put("sequenceNext", nextNumber);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            log.error("❌ Test generate error: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok(response);
    }

    // ======================== TEST CREATE - FIXED ========================

    @PostMapping("/test-create")
    public ResponseEntity<Map<String, Object>> testCreateTrip() {
        Map<String, Object> response = new HashMap<>();
        try {
            // Create a simple trip with hardcoded number
            Trip trip = new Trip();
            trip.setTripNumber("TRP-TEST-001");
            trip.setCustomerId(1L);
            
            // Get vehicle
            vehicleRepository.findById(1L).ifPresent(trip::setVehicle);
            
            // Use String status instead of enum
            trip.setStatus(STATUS_PLANNED);
            trip.setOriginLocation("Test Origin");
            trip.setDestinationLocation("Test Destination");
            trip.setCreatedAt(LocalDateTime.now());
            trip.setLastStatusUpdate(LocalDateTime.now());
            trip.setIsActive(true);
            
            Trip saved = tripRepository.save(trip);
            response.put("success", true);
            response.put("trip", tripResponseMapper.toResponse(saved));
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            log.error("❌ Test create error: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok(response);
    }

    // ======================== USER ENDPOINTS ========================

    /**
     * Comprehensive user verification endpoint
     */
    @GetMapping("/user/verify")
    public ResponseEntity<Map<String, Object>> verifyUser(@RequestParam String email) {
        try {
            Optional<AppUser> userOptional = appUserRepository.findByEmailIgnoreCase(email);

            if (userOptional.isEmpty()) {
                return ResponseEntity.ok(createResponse(false,
                        Map.of("message", "User not found in database")));
            }

            AppUser user = userOptional.get();
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            Map<String, Object> userInfo = extractUserInfo(user, userDetails);

            return ResponseEntity.ok(createResponse(true, userInfo));

        } catch (Exception e) {
            log.error("Error verifying user: {}", email, e);
            return ResponseEntity.badRequest().body(createErrorResponse(
                    "USER_VERIFICATION_ERROR", e.getMessage()));
        }
    }

    /**
     * Simple user existence check
     */
    @GetMapping("/user/exists")
    public ResponseEntity<Map<String, Object>> userExists(@RequestParam String email) {
        try {
            Optional<AppUser> user = appUserRepository.findByEmailIgnoreCase(email);

            Map<String, Object> result = new HashMap<>();
            result.put("exists", user.isPresent());

            if (user.isPresent()) {
                AppUser appUser = user.get();
                result.put("userInfo", Map.of(
                        "id", appUser.getId(),
                        "email", appUser.getEmail(),
                        "enabled", appUser.isEnabled()
                ));
            } else {
                result.put("message", "User not found");
            }

            return ResponseEntity.ok(createResponse(true, result));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(
                    "CHECK_ERROR", e.getMessage()));
        }
    }

    // ======================== TOKEN ENDPOINTS ========================

    /**
     * Test JWT generation for a user
     */
    @GetMapping("/token/generate")
    public ResponseEntity<Map<String, Object>> generateTestToken(@RequestParam String email) {
        try {
            Optional<AppUser> userOptional = appUserRepository.findByEmailIgnoreCase(email);

            if (userOptional.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse(
                        "USER_NOT_FOUND", "User with email " + email + " not found"));
            }

            AppUser user = userOptional.get();
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            String token = jwtService.generateToken(userDetails, user.getId(), user.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("userId", user.getId());
            response.put("email", user.getEmail());
            response.put("userDetails", Map.of(
                    "username", userDetails.getUsername(),
                    "authorities", userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList()),
                    "enabled", userDetails.isEnabled()
            ));
            response.put("tokenInfo", Map.of(
                    "extractedEmail", jwtService.extractEmail(token),
                    "extractedUserId", jwtService.extractUserId(token),
                    "extractedAuthorities", jwtService.extractAuthorities(token),
                    "isValid", jwtService.isValid(token)
            ));
            response.put("tokenPreview", token.substring(0, Math.min(50, token.length())) + "...");

            return ResponseEntity.ok(createResponse(true, response));

        } catch (Exception e) {
            log.error("Error generating token for user: {}", email, e);
            return ResponseEntity.badRequest().body(createErrorResponse(
                    "TOKEN_GENERATION_ERROR", e.getMessage()));
        }
    }

    /**
     * JWT Token analysis endpoint
     */
    @GetMapping("/token/analyze")
    public ResponseEntity<Map<String, Object>> analyzeToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(createErrorResponse(
                    "INVALID_TOKEN_FORMAT",
                    "Authorization header must start with 'Bearer '"));
        }

        String token = authHeader.substring(7);

        try {
            Map<String, Object> analysis = new HashMap<>();

            // Basic token validation
            analysis.put("tokenLength", token.length());
            analysis.put("tokenValid", jwtService.isValid(token));

            // Extract claims
            String email = jwtService.extractEmail(token);
            analysis.put("extractedEmail", email);
            analysis.put("extractedAuthorities", jwtService.extractAuthorities(token));
            analysis.put("extractedUserId", jwtService.extractUserId(token));

            // User details verification
            if (email != null) {
                try {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    analysis.put("userExists", true);
                    analysis.put("userAuthorities", userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList()));

                    Optional<AppUser> appUser = appUserRepository.findByEmailIgnoreCase(email);
                    appUser.ifPresent(user -> {
                        analysis.put("userEntity", extractUserInfoSimple(user));
                    });
                } catch (Exception e) {
                    analysis.put("userExists", false);
                    analysis.put("userLoadError", e.getMessage());
                }
            }

            // Token structure analysis
            Map<String, Object> tokenStructure = analyzeTokenStructure(token);
            analysis.put("tokenStructure", tokenStructure);

            return ResponseEntity.ok(createResponse(true, analysis));

        } catch (Exception e) {
            log.error("Token analysis error", e);
            return ResponseEntity.badRequest().body(createErrorResponse(
                    "TOKEN_ANALYSIS_ERROR", e.getMessage()));
        }
    }

    /**
     * Decode and validate a token
     */
    @PostMapping("/token/decode")
    public ResponseEntity<Map<String, Object>> decodeToken(@RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");

            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse(
                        "MISSING_TOKEN", "Token is required"));
            }

            Map<String, Object> decoded = new HashMap<>();

            decoded.put("userId", jwtService.extractUserId(token));
            decoded.put("email", jwtService.extractEmail(token));
            decoded.put("authorities", jwtService.extractAuthorities(token));
            decoded.put("isValid", jwtService.isValid(token));

            Map<String, Object> tokenStructure = analyzeTokenStructure(token);
            decoded.put("tokenStructure", tokenStructure);

            return ResponseEntity.ok(createResponse(true, decoded));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(
                    "DECODE_ERROR", e.getMessage()));
        }
    }

    // ======================== SEQUENCE & TRIP NUMBER ENDPOINTS ========================

    @GetMapping("/trip-number")
    public ResponseEntity<Map<String, Object>> testTripNumber() {
        Map<String, Object> response = new HashMap<>();
        try {
            String tripNumber = tripNumberGenerator.generate();
            response.put("success", true);
            response.put("tripNumber", tripNumber);
            response.put("message", "Trip number generated successfully");
            log.info("✅ Test generated trip number: {}", tripNumber);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            log.error("❌ Error generating trip number: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sequence")
    public ResponseEntity<Map<String, Object>> testSequence() {
        Map<String, Object> response = new HashMap<>();
        try {
            String year = String.valueOf(Year.now().getValue());
            String result = sequenceService.generateFormattedSequence("trip", "TRP", year, 3);
            response.put("success", true);
            response.put("sequence", result);
            response.put("year", year);
            log.info("✅ Test generated sequence: {}", result);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            log.error("❌ Error generating sequence: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok(response);
    }

    // ======================== AUTH SIMULATION ========================

    /**
     * Test authentication flow - simulates login
     */
    @PostMapping("/auth/simulate-login")
    public ResponseEntity<Map<String, Object>> simulateLogin(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse(
                        "MISSING_EMAIL", "Email is required"));
            }

            Optional<AppUser> userOptional = appUserRepository.findByEmailIgnoreCase(email);

            if (userOptional.isEmpty()) {
                return ResponseEntity.ok(createResponse(false,
                        Map.of("message", "User not found")));
            }

            AppUser user = userOptional.get();
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            Map<String, Object> loginSimulation = new HashMap<>();
            loginSimulation.put("userFound", true);
            loginSimulation.put("userDetails", Map.of(
                    "username", userDetails.getUsername(),
                    "enabled", userDetails.isEnabled(),
                    "accountNonExpired", userDetails.isAccountNonExpired(),
                    "accountNonLocked", userDetails.isAccountNonLocked(),
                    "credentialsNonExpired", userDetails.isCredentialsNonExpired(),
                    "authorities", userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList())
            ));

            loginSimulation.put("appUser", Map.of(
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "enabled", user.isEnabled()
            ));

            loginSimulation.put("jwtPayload", Map.of(
                    "subject", user.getId().toString(),
                    "email", user.getEmail(),
                    "authorities", userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                            .collect(Collectors.toList())
            ));

            return ResponseEntity.ok(createResponse(true, loginSimulation));

        } catch (Exception e) {
            log.error("Login simulation error", e);
            return ResponseEntity.badRequest().body(createErrorResponse(
                    "SIMULATION_ERROR", e.getMessage()));
        }
    }

    // ======================== PRIVATE HELPER METHODS ========================

    private Map<String, Object> extractUserInfo(AppUser user, UserDetails userDetails) {
        Map<String, Object> info = new HashMap<>();

        info.put("id", user.getId());
        info.put("email", user.getEmail());
        info.put("enabled", user.isEnabled());

        // Check for role if exists
        try {
            Method getRoleMethod = user.getClass().getMethod("getRole");
            Object role = getRoleMethod.invoke(user);
            info.put("role", role);
        } catch (NoSuchMethodException e) {
            info.put("role", "Not available");
        } catch (Exception e) {
            info.put("role", "Error: " + e.getMessage());
        }

        info.put("accountNonExpired", userDetails.isAccountNonExpired());
        info.put("accountNonLocked", userDetails.isAccountNonLocked());
        info.put("credentialsNonExpired", userDetails.isCredentialsNonExpired());
        info.put("authorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return info;
    }

    private Map<String, Object> extractUserInfoSimple(AppUser user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("email", user.getEmail());
        info.put("enabled", user.isEnabled());
        return info;
    }

    private Map<String, Object> analyzeTokenStructure(String token) {
        Map<String, Object> structure = new HashMap<>();

        try {
            String[] parts = token.split("\\.");
            structure.put("partsCount", parts.length);
            structure.put("hasHeader", parts.length > 0);
            structure.put("hasPayload", parts.length > 1);
            structure.put("hasSignature", parts.length > 2);

            if (parts.length >= 2) {
                try {
                    String header = new String(Base64.getUrlDecoder().decode(parts[0]));
                    String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

                    structure.put("header", header);
                    structure.put("payload", payload);

                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        structure.put("headerJson", mapper.readTree(header));
                        structure.put("payloadJson", mapper.readTree(payload));
                    } catch (Exception e) {
                        structure.put("jsonParseError", e.getMessage());
                    }

                } catch (IllegalArgumentException e) {
                    structure.put("decodingError", "Base64 URL decoding failed: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            structure.put("analysisError", e.getMessage());
        }

        return structure;
    }

    private Map<String, Object> createResponse(boolean success, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("data", data);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    private Map<String, Object> createErrorResponse(String errorCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", Map.of(
                "code", errorCode,
                "message", message
        ));
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
