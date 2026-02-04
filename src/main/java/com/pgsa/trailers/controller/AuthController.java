package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.AppUserDTO;
import com.pgsa.trailers.entity.security.AppUser;
import com.pgsa.trailers.repository.AppUserRepository;
import com.pgsa.trailers.service.security.JwtService;
import com.pgsa.trailers.service.security.UserService;
import com.pgsa.trailers.service.security.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:8081", "http://localhost:5173", "https://trailers-backend.onrender.com","https://trailers-1.onrender.com"})
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;
    private final AppUserRepository userRepository;
    private final DataSource dataSource; // Added for database info
    
    @Value("${spring.datasource.url:Not configured}")
    private String datasourceUrl;
    
    @Value("${spring.datasource.username:Not configured}")
    private String datasourceUsername;

    // ========== DATABASE INFO ENDPOINTS ==========
    
    @GetMapping("/db-info")
    public ResponseEntity<?> getDatabaseInfo() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            Map<String, Object> dbInfo = new HashMap<>();
            dbInfo.put("databaseProductName", metaData.getDatabaseProductName());
            dbInfo.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            dbInfo.put("url", metaData.getURL());
            dbInfo.put("userName", metaData.getUserName());
            
            // From application.properties
            dbInfo.put("configuredUrl", datasourceUrl);
            dbInfo.put("configuredUsername", datasourceUsername);
            
            log.info("=== DATABASE CONNECTION INFO ===");
            log.info("Database: {}", metaData.getDatabaseProductName());
            log.info("Version: {}", metaData.getDatabaseProductVersion());
            log.info("JDBC URL: {}", metaData.getURL());
            log.info("Connected as: {}", metaData.getUserName());
            log.info("Configured URL: {}", datasourceUrl);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "database", dbInfo
            ));
            
        } catch (Exception e) {
            log.error("Error getting database info", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "Failed to get database info: " + e.getMessage(),
                "configuredUrl", datasourceUrl,
                "configuredUsername", datasourceUsername
            ));
        }
    }
    
    @GetMapping("/db-check")
    public ResponseEntity<?> checkDatabaseTables() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            
            List<String> tableList = new ArrayList<>();
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                tableList.add(tableName);
            }
            
            // Check for specific tables
            Map<String, Boolean> requiredTables = new HashMap<>();
            requiredTables.put("app_user", tableList.stream().anyMatch(t -> t.equalsIgnoreCase("app_user")));
            requiredTables.put("expense", tableList.stream().anyMatch(t -> t.equalsIgnoreCase("expense")));
            requiredTables.put("fuel_slip", tableList.stream().anyMatch(t -> t.equalsIgnoreCase("fuel_slip")));
            requiredTables.put("trip", tableList.stream().anyMatch(t -> t.equalsIgnoreCase("trip")));
            requiredTables.put("vehicle", tableList.stream().anyMatch(t -> t.equalsIgnoreCase("vehicle")));
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "totalTables", tableList.size(),
                "tables", tableList,
                "requiredTables", requiredTables,
                "database", metaData.getDatabaseProductName()
            ));
            
        } catch (Exception e) {
            log.error("Database check failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "Database check failed: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("service", "Auth Service");
        health.put("version", "1.0.0");
        
        try (Connection connection = dataSource.getConnection()) {
            health.put("database", "CONNECTED");
            health.put("databaseProduct", connection.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            health.put("database", "DISCONNECTED");
            health.put("databaseError", e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }

    // ========== AUTH ENDPOINTS ==========
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // DEBUG: Check user before authentication
        try {
            Optional<AppUser> userCheck = userRepository.findByEmailIgnoreCase(request.getEmail());
            log.info("👤 User found: {}", userCheck.isPresent());
            if (userCheck.isPresent()) {
                AppUser u = userCheck.get();
                boolean pwdMatch = passwordEncoder.matches(request.getPassword(), u.getPasswordHash());
                log.info("🔑 Password matches: {}", pwdMatch);
                log.info("✅ User enabled: {}", u.isEnabled());
                log.info("📋 Roles count: {}", u.getRoles().size());
            }
        } catch (Exception e) {
            log.error("❌ Pre-auth check failed", e);
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            AppUserDTO userDTO = userService.getUserByEmail(request.getEmail());

            String token = jwtService.generateToken(userDetails, userDTO.getId(), userDTO.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "token", token,
                    "user", userDTO
            ));

        } catch (BadCredentialsException ex) {
            log.warn("Invalid credentials for email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "error", "Invalid email or password"
            ));
        } catch (Exception ex) {
            log.error("Unexpected login error for email: {}", request.getEmail(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Authentication failed"
            ));
        }
    }

    @GetMapping("/check-token")
    public ResponseEntity<?> checkToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("status", "error");
            response.put("message", "No Bearer token found");
            return ResponseEntity.ok(response);
        }

        String token = authHeader.substring(7);

        try {
            // Extract info from token
            String email = jwtService.extractEmail(token);
            List<String> authorities = jwtService.extractAuthorities(token);
            Long userId = jwtService.extractUserId(token);
            boolean isValid = jwtService.isValid(token);

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            response.put("status", "success");
            response.put("tokenInfo", Map.of(
                    "email", email,
                    "userId", userId,
                    "isValid", isValid,
                    "jwtAuthorities", authorities,
                    "userDetailsAuthorities", userDetails.getAuthorities()
                            .stream()
                            .map(GrantedAuthority::getAuthority)
                            .toList()
            ));

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Token error: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(authentication)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "error", "Not authenticated"
                ));
            }

            String email = authentication.getName();
            var userEntity = userService.getUserEntityByEmail(email);

            if (!passwordEncoder.matches(request.getOldPassword(), userEntity.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "success", false,
                        "error", "Current password is incorrect"
                ));
            }

            userEntity.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userService.saveUser(userEntity);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Password updated successfully"
            ));
        } catch (Exception ex) {
            log.error("Error updating password", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Failed to update password"
            ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            log.info("=== /me ENDPOINT CALLED ===");
            
            // Debug: Log the incoming auth header
            if (authHeader != null) {
                log.info("Auth Header: {}", authHeader);
                if (authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    log.info("Token length: {}", token.length());
                    
                    try {
                        String email = jwtService.extractEmail(token);
                        log.info("Token email: {}", email);
                        log.info("Token valid: {}", jwtService.isValid(token));
                    } catch (Exception e) {
                        log.error("Token validation error: {}", e.getMessage());
                    }
                }
            }
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.info("Authentication: {}", authentication);
            log.info("Principal: {}", authentication != null ? authentication.getPrincipal() : "null");
            log.info("Authenticated: {}", authentication != null ? authentication.isAuthenticated() : "false");
            
            if (!isAuthenticated(authentication)) {
                log.warn("User not authenticated!");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "error", "Not authenticated"
                ));
            }

            String email = authentication.getName();
            log.info("Authenticated email: {}", email);
            AppUserDTO userDTO = userService.getUserByEmail(email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", userDTO
            ));
        } catch (Exception ex) {
            log.error("Error fetching current user", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Failed to get user info: " + ex.getMessage()
            ));
        }
    }
    
    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "success", false,
                "error", "No token provided"
            ));
        }

        String token = authHeader.substring(7);
        
        try {
            String email = jwtService.extractEmail(token);
            boolean isValid = jwtService.isValid(token);
            List<String> authorities = jwtService.extractAuthorities(token);
            Long userId = jwtService.extractUserId(token);
            
            // Try to load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "valid", isValid,
                "email", email,
                "userId", userId,
                "authorities", authorities,
                "userDetails", Map.of(
                    "username", userDetails.getUsername(),
                    "enabled", userDetails.isEnabled(),
                    "authorities", userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList()
                )
            ));
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "success", false,
                "error", "Invalid token: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/verify-token")
    public ResponseEntity<?> verifyToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.info("=== VERIFY TOKEN ENDPOINT ===");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "No token provided",
                    "success", false
            ));
        }

        String token = authHeader.substring(7);

        try {
            String email = jwtService.extractEmail(token);
            List<String> authorities = jwtService.extractAuthorities(token);
            Long userId = jwtService.extractUserId(token);
            boolean isValid = jwtService.isValid(token);

            // Also try to load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "tokenInfo", Map.of(
                            "email", email,
                            "userId", userId,
                            "authoritiesFromJwt", authorities,
                            "isValid", isValid,
                            "userDetailsAuthorities", userDetails.getAuthorities().toString(),
                            "userEnabled", userDetails.isEnabled()
                    )
            ));
        } catch (Exception e) {
            log.error("Token verification failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Invalid token: " + e.getMessage(),
                    "success", false
            ));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Logged out successfully");
    }
    
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "status", "OK",
                "service", "Authentication Service",
                "timestamp", System.currentTimeMillis()
        ));
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null &&
                authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal());
    }

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data
    public static class PasswordChangeRequest {
        private String oldPassword;
        private String newPassword;
    }
}
