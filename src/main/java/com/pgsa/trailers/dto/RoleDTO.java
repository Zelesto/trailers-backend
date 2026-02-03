package com.pgsa.trailers.dto;

import lombok.Data;

import java.util.Set;

@Data
public class RoleDTO {
    private Long id;
    private String name;
    private String description;

    // Include permissions for richer RBAC info
    private Set<PermissionDTO> permissions;
}
