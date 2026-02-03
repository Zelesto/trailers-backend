package com.pgsa.trailers.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class AppUserDTO {
    private Long id;
    private String username;
    private String email;
    private boolean enabled;

    // Roles now include permissions
    private Set<RoleDTO> roles;

    // Audit fields from AuditedEntity
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
