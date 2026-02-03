package com.pgsa.trailers.controller;

import com.pgsa.trailers.entity.security.AppUser;
import com.pgsa.trailers.repository.AppUserRepository;
import com.pgsa.trailers.service.security.CustomUserDetailsService;
import com.pgsa.trailers.service.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
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

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(createResponse(true, Map.of(
                "status", "UP",
                "service", "Test Controller",
                "timestamp", System.currentTimeMillis()
        )));
    }

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

                    // Try to get the AppUser entity
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
     * Test JWT generation for a user - UPDATED for your JwtService
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

            // Generate token using the correct parameters for YOUR JwtService
            // generateToken(UserDetails userDetails, Long userId, String email)
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

            // Extract all information from the token
            decoded.put("userId", jwtService.extractUserId(token));
            decoded.put("email", jwtService.extractEmail(token));
            decoded.put("authorities", jwtService.extractAuthorities(token));
            decoded.put("isValid", jwtService.isValid(token));

            // Manual token structure analysis
            Map<String, Object> tokenStructure = analyzeTokenStructure(token);
            decoded.put("tokenStructure", tokenStructure);

            return ResponseEntity.ok(createResponse(true, decoded));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse(
                    "DECODE_ERROR", e.getMessage()));
        }
    }

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

            // This simulates what should happen during login
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

            // Show what would be included in the JWT
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

    // ========== HELPER METHODS ==========

    private Map<String, Object> extractUserInfo(AppUser user, UserDetails userDetails) {
        Map<String, Object> info = new HashMap<>();

        info.put("id", user.getId());
        info.put("email", user.getEmail());
        info.put("enabled", user.isEnabled());

        // Check for role if exists
        try {
            java.lang.reflect.Method getRoleMethod = user.getClass().getMethod("getRole");
            Object role = getRoleMethod.invoke(user);
            info.put("role", role);
        } catch (NoSuchMethodException e) {
            info.put("role", "Not available");
        } catch (Exception e) {
            info.put("role", "Error: " + e.getMessage());
        }

        // Add UserDetails info
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

                    // Try to parse JSON for better readability
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
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