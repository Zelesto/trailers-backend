package com.pgsa.trailers.service.util;

import com.pgsa.trailers.entity.security.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    public AppUser getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user");
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof AppUser user) {
            return user;
        }

        throw new IllegalStateException("Authenticated principal is not AppUser");
    }
}

