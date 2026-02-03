package com.pgsa.trailers.entity.security;

import com.pgsa.trailers.dto.AppUserDTO;
import com.pgsa.trailers.dto.PermissionDTO;
import com.pgsa.trailers.dto.RoleDTO;
import com.pgsa.trailers.entity.security.AppUser;
import com.pgsa.trailers.entity.security.Role;
import com.pgsa.trailers.entity.security.Permission;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public AppUserDTO toDTO(AppUser user) {
        if (user == null) return null;

        AppUserDTO dto = new AppUserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        if (user.getRoles() != null) {
            dto.setRoles(user.getRoles().stream()
                    .map(this::toRoleDTO)
                    .collect(Collectors.toSet()));
        }

        return dto;
    }

    public RoleDTO toRoleDTO(Role role) {
        if (role == null) return null;

        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());

        if (role.getPermissions() != null) {
            Set<PermissionDTO> perms = role.getPermissions().stream()
                    .map(this::toPermissionDTO)
                    .collect(Collectors.toSet());
            dto.setPermissions(perms);
        }

        return dto;
    }

    public PermissionDTO toPermissionDTO(Permission permission) {
        if (permission == null) return null;

        PermissionDTO dto = new PermissionDTO();
        dto.setId(permission.getId());
        dto.setResource(permission.getResource());
        dto.setAction(permission.getAction());
        return dto;
    }
}
