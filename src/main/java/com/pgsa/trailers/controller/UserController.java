package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.AppUserDTO;
import com.pgsa.trailers.dto.UserRequest;
import com.pgsa.trailers.service.security.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // --- Create ---
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AppUserDTO> createUser(@Valid @RequestBody UserRequest request) {
        applyDefaults(request);
        AppUserDTO createdUser = userService.createUser(request);
        return ResponseEntity.ok(createdUser);
    }

    // --- Read single by ID ---
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AppUserDTO> getUserById(@PathVariable Long id) {
        AppUserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    // --- Read all ---
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AppUserDTO>> getAllUsers() {
        List<AppUserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // --- Update ---
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AppUserDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request) {
        applyDefaults(request);
        AppUserDTO updatedUser = userService.updateUser(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    // --- Delete ---
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // --- Current authenticated user ---
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppUserDTO> getCurrentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        String email = authentication.getName();
        AppUserDTO user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    // ---------------- Helpers ----------------

    /**
     * Apply safe defaults if fields are missing in the request.
     */
    private void applyDefaults(UserRequest request) {
        // Default enabled flag
        if (request.isEnabled() == false) {
            request.setEnabled(true);
        }

        // Default role assignment if none provided
        if (request.getRoleIds() == null || request.getRoleIds().isEmpty()) {
            // fallback to USER role (must exist in DB)
            request.setRoleIds(Set.of(2L)); // e.g., DISPATCHER or USER role ID
        }

        // Default password handling for updates
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            // For create, password is required (service enforces this).
            // For update, leave it null so service won't overwrite.
        }
    }
}
