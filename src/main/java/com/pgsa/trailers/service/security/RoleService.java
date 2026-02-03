package com.pgsa.trailers.service.security;

import com.pgsa.trailers.dto.RoleDTO;
import com.pgsa.trailers.entity.security.Permission;
import com.pgsa.trailers.entity.security.Role;
import com.pgsa.trailers.repository.PermissionRepository;
import com.pgsa.trailers.repository.RoleRepository;
import com.pgsa.trailers.entity.security.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserMapper userMapper; // reuse mapper for RoleDTO/PermissionDTO

    // ---------------- CRUD ----------------

    public RoleDTO createRole(String name, String description, Set<Long> permissionIds) {
        if (roleRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Role already exists: " + name);
        }

        Role role = new Role();
        role.setName(name);
        role.setDescription(description);

        if (permissionIds != null && !permissionIds.isEmpty()) {
            role.setPermissions(resolvePermissions(permissionIds));
        }

        Role saved = roleRepository.save(role);
        return userMapper.toRoleDTO(saved);
    }

    public RoleDTO updateRole(Long id, String name, String description, Set<Long> permissionIds) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + id));

        if (!Objects.equals(role.getName(), name)) {
            roleRepository.findByName(name).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException("Role name already exists: " + name);
                }
            });
            role.setName(name);
        }

        role.setDescription(description);

        if (permissionIds != null) {
            role.setPermissions(resolvePermissions(permissionIds));
        }

        Role updated = roleRepository.save(role);
        return userMapper.toRoleDTO(updated);
    }

    public RoleDTO getRoleById(Long id) {
        return roleRepository.findById(id)
                .map(userMapper::toRoleDTO)
                .orElseThrow(() -> new IllegalArgumentException("Role not found with id: " + id));
    }

    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(userMapper::toRoleDTO)
                .collect(Collectors.toList());
    }

    public void deleteRole(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new IllegalArgumentException("Role not found with id: " + id);
        }
        roleRepository.deleteById(id);
    }

    // ---------------- Helpers ----------------

    private Set<Permission> resolvePermissions(Set<Long> permissionIds) {
        List<Permission> permissions = permissionRepository.findAllById(permissionIds);
        if (permissions.size() != permissionIds.size()) {
            throw new IllegalArgumentException("One or more permissions not found");
        }
        return new HashSet<>(permissions);
    }
}
