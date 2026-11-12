package com.pgsa.trailers.service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String requestPath = request.getServletPath();
        final String requestMethod = request.getMethod();

        log.debug("🛡️ JWT Filter: {} {}", requestMethod, requestPath);

        // Skip authentication for certain endpoints
        if (shouldNotFilter(request)) {
            log.debug("⏩ Skipping JWT filter for: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract token from request
            String token = extractTokenFromRequest(request);

            if (!StringUtils.hasText(token)) {
                log.warn("❌ No JWT token found for: {} {}", requestMethod, requestPath);
                handleUnauthorized(response, "Missing authentication token");
                return;
            }

            // Validate token
            if (!jwtService.isValid(token)) {
                log.warn("❌ Invalid JWT token for: {} {}", requestMethod, requestPath);
                handleUnauthorized(response, "Invalid authentication token");
                return;
            }

            // Extract username from token
            String username = jwtService.extractUsername(token);
            if (!StringUtils.hasText(username)) {
                log.warn("❌ No username in JWT token for: {} {}", requestMethod, requestPath);
                handleUnauthorized(response, "Invalid token payload");
                return;
            }

            log.debug("📧 Extracted username from token: {}", username);

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (userDetails == null) {
                log.error("❌ User not found: {}", username);
                handleUnauthorized(response, "User not found");
                return;
            }

            log.debug("👤 UserDetails loaded: {}, authorities: {}",
                    userDetails.getUsername(), userDetails.getAuthorities());

            // Extract authorities from token
            List<String> roles = jwtService.extractAuthorities(token);
            log.debug("🎯 Authorities from JWT: {}", roles);

            // Create authentication token
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()); // Use authorities from UserDetails

            // Set authentication details
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("✅ Authentication set for user: {}", username);

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("⚠️ JWT token expired for: {} {} - {}", requestMethod, requestPath, e.getMessage());
            handleUnauthorized(response, "Token expired");
            return;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("⚠️ Malformed JWT token for: {} {} - {}", requestMethod, requestPath, e.getMessage());
            handleUnauthorized(response, "Malformed token");
            return;
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.warn("⚠️ Unsupported JWT token for: {} {} - {}", requestMethod, requestPath, e.getMessage());
            handleUnauthorized(response, "Unsupported token");
            return;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("⚠️ Invalid JWT signature for: {} {} - {}", requestMethod, requestPath, e.getMessage());
            handleUnauthorized(response, "Invalid token signature");
            return;
        } catch (Exception e) {
            log.error("🚨 Unexpected error in JWT filter for: {} {} - {}",
                    requestMethod, requestPath, e.getMessage(), e);
            handleUnauthorized(response, "Authentication error");
            return;
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7).trim();
        }

        return null;
    }

    /**
     * Handle unauthorized requests
     */
    private void handleUnauthorized(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = String.format(
                "{\"success\": false, \"error\": \"%s\", \"timestamp\": \"%s\"}",
                message,
                java.time.LocalDateTime.now().toString()
        );

        response.getWriter().write(jsonResponse);
    }

    /**
     * Determine if filter should be skipped for certain paths
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String path = request.getRequestURI();
        if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        // List of public endpoints that don't require authentication
        List<String> publicEndpoints = List.of(
                "/api/auth",
                "/api/test",
                "/api/public/",
                "/swagger-ui/",
                "/v3/api-docs",
                "/swagger-ui.html",
                "/api-docs",
                "/actuator/health",
                "/error"
        );

        // Check if path starts with any public endpoint
        boolean shouldSkip = publicEndpoints.stream().anyMatch(path::startsWith);

        if (shouldSkip) {
            log.trace("⏭️ Skipping JWT filter for public endpoint: {}", path);
        }

        return shouldSkip;
    }

    /**
     * Clean up security context after request
     */
    @Override
    protected void doFilterNestedErrorDispatch(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
