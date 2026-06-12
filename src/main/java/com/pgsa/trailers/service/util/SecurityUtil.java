package com.pgsa.trailers.service.util;

import com.pgsa.trailers.entity.security.AppUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SecurityUtil {

    /**
     * Get the currently authenticated user
     */
    public AppUser getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            log.error("No authenticated user found in security context");
            throw new IllegalStateException("No authenticated user");
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof AppUser user) {
            log.debug("Authenticated user: {} (ID: {})", user.getUsername(), user.getId());
            return user;
        }

        log.error("Authenticated principal is not AppUser: {}", principal.getClass().getName());
        throw new IllegalStateException("Authenticated principal is not AppUser");
    }

    /**
     * Get the current user ID
     */
    public Long getCurrentUserId() {
        AppUser user = getAuthenticatedUser();
        return user.getId();
    }

    /**
     * Get the current username
     */
    public String getCurrentUsername() {
        AppUser user = getAuthenticatedUser();
        return user.getUsername();
    }

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_" + role) ||
                                              grantedAuthority.getAuthority().equals(role));
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Check if user is dispatcher
     */
    public boolean isDispatcher() {
        return hasRole("DISPATCHER");
    }

    /**
     * Check if user is driver
     */
    public boolean isDriver() {
        return hasRole("DRIVER");
    }

    /**
     * Get authentication token (for auditing/logging)
     */
    public String getAuthenticationDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "No authentication";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(auth.getName());
        sb.append(", Authenticated: ").append(auth.isAuthenticated());
        sb.append(", Authorities: ").append(auth.getAuthorities());
        
        return sb.toString();
    }

    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth.getPrincipal() instanceof String);
    }

    /**
     * Get current user ID safely (returns null if not authenticated)
     */
    public Long getCurrentUserIdSafe() {
        try {
            return getCurrentUserId();
        } catch (IllegalStateException e) {
            log.warn("Failed to get current user ID: {}", e.getMessage());
            return null;
        }
    }
}
