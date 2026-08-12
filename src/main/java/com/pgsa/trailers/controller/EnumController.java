// src/main/java/com/pgsa/trailers/controller/EnumManagementController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.CreateCustomEnumRequest;
import com.pgsa.trailers.dto.CustomEnumDto;
import com.pgsa.trailers.service.EnumManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/enums")
@RequiredArgsConstructor
@Slf4j
public class EnumManagementController {

    private final EnumManagementService enumManagementService;

    @GetMapping("/{enumType}")
    public ResponseEntity<List<CustomEnumDto>> getEnumsByType(@PathVariable String enumType) {
        try {
            List<CustomEnumDto> enums = enumManagementService.getEnumsByType(enumType);
            return ResponseEntity.ok(enums);
        } catch (Exception e) {
            log.error("Error getting enums for type {}: {}", enumType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<CustomEnumDto>> getEnums(
            @RequestParam(required = false) String enumType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.Direction.fromString(sortDir), sortBy);
            Page<CustomEnumDto> enums = enumManagementService.getEnumsPaginated(enumType, pageable);
            return ResponseEntity.ok(enums);
        } catch (Exception e) {
            log.error("Error getting paginated enums: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/types")
    public ResponseEntity<List<String>> getEnumTypes() {
        try {
            List<String> types = enumManagementService.getEnumTypes();
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            log.error("Error getting enum types: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Enum controller is working");
        response.put("message", "Success");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/custom")
    public ResponseEntity<CustomEnumDto> addCustomEnum(@Valid @RequestBody CreateCustomEnumRequest request) {
        try {
            Long userId = getCurrentUserId();
            
            CustomEnumDto dto = CustomEnumDto.builder()
                    .enumType(request.getEnumType().toUpperCase())
                    .value(request.getValue().toUpperCase())
                    .displayName(request.getDisplayName())
                    .description(request.getDescription())
                    .icon(request.getIcon())
                    .color(request.getColor())
                    .sortOrder(request.getSortOrder())
                    .metadata(request.getMetadata())
                    .build();
            
            CustomEnumDto created = enumManagementService.addCustomEnum(dto, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error adding enum: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error adding enum: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/custom/{id}")
    public ResponseEntity<CustomEnumDto> updateCustomEnum(
            @PathVariable Long id,
            @Valid @RequestBody CreateCustomEnumRequest request) {
        try {
            Long userId = getCurrentUserId();
            
            CustomEnumDto dto = CustomEnumDto.builder()
                    .enumType(request.getEnumType().toUpperCase())
                    .value(request.getValue().toUpperCase())
                    .displayName(request.getDisplayName())
                    .description(request.getDescription())
                    .icon(request.getIcon())
                    .color(request.getColor())
                    .sortOrder(request.getSortOrder())
                    .metadata(request.getMetadata())
                    .build();
            
            CustomEnumDto updated = enumManagementService.updateCustomEnum(id, dto, userId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error updating enum: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating enum: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/custom/{id}")
    public ResponseEntity<Void> deleteCustomEnum(
            @PathVariable Long id,
            @RequestParam String enumType) {
        try {
            enumManagementService.deleteCustomEnum(id, enumType);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Validation error deleting enum: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error deleting enum: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/custom/{id}/toggle")
    public ResponseEntity<CustomEnumDto> toggleEnumStatus(
            @PathVariable Long id,
            @RequestParam String enumType) {
        try {
            Long userId = getCurrentUserId();
            CustomEnumDto updated = enumManagementService.toggleEnumStatus(id, enumType, userId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error toggling enum: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error toggling enum: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Long getCurrentUserId() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            // In a real implementation, look up user by email
            return 1L;
        } catch (Exception e) {
            return 1L;
        }
    }
}
