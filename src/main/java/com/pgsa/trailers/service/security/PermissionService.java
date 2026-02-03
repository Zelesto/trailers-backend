package com.pgsa.trailers.service.security;

import com.pgsa.trailers.dto.PermissionDTO;
import com.pgsa.trailers.entity.security.Permission;
import com.pgsa.trailers.entity.security.UserMapper;
import com.pgsa.trailers.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final UserMapper userMapper;

    // ---------------- CRUD ----------------

    public PermissionDTO createPermission(String resource, String action) {
        // Enforce uniqueness
        permissionRepository.findByResourceAndAction(resource, action).ifPresent(existing -> {
            throw new IllegalArgumentException("Permission already exists for resource: " + resource + " and action: " + action);
        });

        Permission permission = new Permission();
        permission.setResource(resource);
        permission.setAction(action);

        Permission saved = permissionRepository.save(permission);
        return userMapper.toPermissionDTO(saved);
    }

    public PermissionDTO updatePermission(Long id, String resource, String action) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with id: " + id));

        // Check uniqueness if resource/action changed
        if (!permission.getResource().equals(resource) || !permission.getAction().equals(action)) {
            permissionRepository.findByResourceAndAction(resource, action).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException("Permission already exists for resource: " + resource + " and action: " + action);
                }
            });
            permission.setResource(resource);
            permission.setAction(action);
        }

        Permission updated = permissionRepository.save(permission);
        return userMapper.toPermissionDTO(updated);
    }

    public PermissionDTO getPermissionById(Long id) {
        return permissionRepository.findById(id)
                .map(userMapper::toPermissionDTO)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found with id: " + id));
    }

    public List<PermissionDTO> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(userMapper::toPermissionDTO)
                .collect(Collectors.toList());
    }

    public void deletePermission(Long id) {
        if (!permissionRepository.existsById(id)) {
            throw new IllegalArgumentException("Permission not found with id: " + id);
        }
        permissionRepository.deleteById(id);
    }

    // ---------------- Helpers ----------------

    public Set<PermissionDTO> getPermissionsByIds(Set<Long> ids) {
        return permissionRepository.findAllById(ids).stream()
                .map(userMapper::toPermissionDTO)
                .collect(Collectors.toSet());
    }
}
