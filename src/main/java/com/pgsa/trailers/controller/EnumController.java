// src/main/java/com/pgsa/trailers/controller/EnumController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.entity.system.EnumMaster;
import com.pgsa.trailers.service.EnumService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enums")
@RequiredArgsConstructor
@Slf4j
public class EnumController {

    private final EnumService enumService;

    // ============================================================
    // READ OPERATIONS
    // ============================================================

    /**
     * Get enums by module and category
     * Example: GET /api/enums/trip/status
     */
    @GetMapping("/{moduleName}/{category}")
    public ResponseEntity<?> getEnums(
            @PathVariable String moduleName,
            @PathVariable String category,
            @RequestParam(required = false) Boolean includeInactive) {
        try {
            List<EnumMaster> enums = enumService.getEnums(moduleName, category, includeInactive);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", enums);
            response.put("count", enums.size());
            response.put("moduleName", moduleName);
            response.put("category", category);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching enums: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get system enums
     * Example: GET /api/enums/trip/status/system
     */
    @GetMapping("/{moduleName}/{category}/system")
    public ResponseEntity<?> getSystemEnums(
            @PathVariable String moduleName,
            @PathVariable String category) {
        try {
            List<EnumMaster> enums = enumService.getSystemEnums(moduleName, category);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", enums,
                "count", enums.size()
            ));
        } catch (Exception e) {
            log.error("Error fetching system enums: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get custom enums
     * Example: GET /api/enums/trip/status/custom
     */
    @GetMapping("/{moduleName}/{category}/custom")
    public ResponseEntity<?> getCustomEnums(
            @PathVariable String moduleName,
            @PathVariable String category) {
        try {
            List<EnumMaster> enums = enumService.getCustomEnums(moduleName, category);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", enums,
                "count", enums.size()
            ));
        } catch (Exception e) {
            log.error("Error fetching custom enums: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get all enums for a module
     * Example: GET /api/enums/module/trip
     */
    @GetMapping("/module/{moduleName}")
    public ResponseEntity<?> getEnumsByModule(@PathVariable String moduleName) {
        try {
            Map<String, List<EnumMaster>> enums = enumService.getEnumsByModule(moduleName);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", enums
            ));
        } catch (Exception e) {
            log.error("Error fetching enums by module: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get enum by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getEnumById(@PathVariable Long id) {
        try {
            EnumMaster enumMaster = enumService.getEnumById(id);
            if (enumMaster == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", "Enum not found"));
            }
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", enumMaster
            ));
        } catch (Exception e) {
            log.error("Error fetching enum: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get enum by module, category, and code
     */
    @GetMapping("/{moduleName}/{category}/{code}")
    public ResponseEntity<?> getEnumByCode(
            @PathVariable String moduleName,
            @PathVariable String category,
            @PathVariable String code) {
        try {
            EnumMaster enumMaster = enumService.getEnum(moduleName, category, code);
            if (enumMaster == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "error", "Enum not found"));
            }
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", enumMaster
            ));
        } catch (Exception e) {
            log.error("Error fetching enum: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get all enum types (categories)
     */
    @GetMapping("/types")
    public ResponseEntity<?> getEnumTypes() {
        try {
            List<String> types = enumService.getEnumTypes();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", types
            ));
        } catch (Exception e) {
            log.error("Error fetching enum types: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get all modules
     */
    @GetMapping("/modules")
    public ResponseEntity<?> getModules() {
        try {
            List<String> modules = enumService.getModules();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", modules
            ));
        } catch (Exception e) {
            log.error("Error fetching modules: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ============================================================
    // CREATE OPERATIONS
    // ============================================================

    /**
     * Create a new custom enum
     */
    @PostMapping
    public ResponseEntity<?> createEnum(@Valid @RequestBody CreateEnumRequest request) {
        try {
            String currentUser = getCurrentUser();
            
            EnumMaster enumMaster = EnumMaster.builder()
                .moduleName(request.getModuleName().toLowerCase())
                .category(request.getCategory().toLowerCase())
                .code(request.getCode().toUpperCase())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .isActive(true)
                .isSystem(false)
                .isEditable(true)
                .isDeletable(true)
                .colorCode(request.getColorCode())
                .iconName(request.getIconName())
                .metadata(request.getMetadata())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();
            
            EnumMaster created = enumService.createEnum(enumMaster);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Enum created successfully",
                "data", created
            ));
        } catch (Exception e) {
            log.error("Error creating enum: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ============================================================
    // UPDATE OPERATIONS
    // ============================================================

    /**
     * Update an existing enum
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEnum(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEnumRequest request) {
        try {
            String currentUser = getCurrentUser();
            
            EnumMaster enumMaster = EnumMaster.builder()
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .sortOrder(request.getSortOrder())
                .isDefault(request.getIsDefault())
                .isActive(request.getIsActive())
                .colorCode(request.getColorCode())
                .iconName(request.getIconName())
                .metadata(request.getMetadata())
                .updatedBy(currentUser)
                .build();
            
            EnumMaster updated = enumService.updateEnum(id, enumMaster);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Enum updated successfully",
                "data", updated
            ));
        } catch (Exception e) {
            log.error("Error updating enum: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ============================================================
    // DELETE OPERATIONS
    // ============================================================

    /**
     * Delete an enum (soft delete by setting isActive = false)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEnum(@PathVariable Long id) {
        try {
            String currentUser = getCurrentUser();
            enumService.deleteEnum(id, currentUser);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Enum deleted successfully"
            ));
        } catch (Exception e) {
            log.error("Error deleting enum: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Toggle enum active status
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleEnumStatus(@PathVariable Long id) {
        try {
            String currentUser = getCurrentUser();
            EnumMaster updated = enumService.toggleEnumStatus(id, currentUser);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Enum status toggled successfully",
                "data", updated
            ));
        } catch (Exception e) {
            log.error("Error toggling enum: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ============================================================
    // REQUEST/UPDATE DTOs
    // ============================================================

    @Data
    public static class CreateEnumRequest {
        @NotBlank(message = "Module name is required")
        private String moduleName;
        
        @NotBlank(message = "Category is required")
        private String category;
        
        @NotBlank(message = "Code is required")
        @Pattern(regexp = "^[A-Z0-9_]+$", message = "Code must be uppercase with underscores")
        private String code;
        
        @NotBlank(message = "Display name is required")
        private String displayName;
        
        private String description;
        private Integer sortOrder;
        private Boolean isDefault;
        private String colorCode;
        private String iconName;
        private Map<String, Object> metadata;
    }

    @Data
    public static class UpdateEnumRequest {
        private String displayName;
        private String description;
        private Integer sortOrder;
        private Boolean isDefault;
        private Boolean isActive;
        private String colorCode;
        private String iconName;
        private Map<String, Object> metadata;
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private String getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : "system";
        } catch (Exception e) {
            return "system";
        }
    }
}
