package com.pgsa.trailers.service.security;

import com.pgsa.trailers.dto.AppUserDTO;
import com.pgsa.trailers.dto.PermissionDTO;
import com.pgsa.trailers.dto.RoleDTO;
import com.pgsa.trailers.dto.UserRequest;
import com.pgsa.trailers.entity.security.AppUser;
import com.pgsa.trailers.entity.security.Role;
import com.pgsa.trailers.repository.AppUserRepository;
import com.pgsa.trailers.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserDTO createUser(UserRequest request) {
        validateUniqueEmail(request.getEmail());
        validateUniqueUsername(request.getUsername());

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        AppUser user = new AppUser();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(request.isEnabled());

        // Assign roles
        Set<Role> roles = resolveRoles(request.getRoleIds());
        user.setRoles(roles);

        AppUser savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    public AppUserDTO updateUser(Long id, UserRequest request) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        // Email update
        if (!Objects.equals(user.getEmail(), request.getEmail())) {
            validateUniqueEmail(request.getEmail(), id);
            user.setEmail(request.getEmail());
        }

        // Username update
        if (!Objects.equals(user.getUsername(), request.getUsername())) {
            validateUniqueUsername(request.getUsername(), id);
            user.setUsername(request.getUsername());
        }

        user.setEnabled(request.isEnabled());

        // Password update
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        // Roles update
        if (request.getRoleIds() != null) {
            user.setRoles(resolveRoles(request.getRoleIds()));
        }

        AppUser updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }

    public AppUserDTO getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    public AppUserDTO getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(this::convertToDTO)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    public List<AppUserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    // ---------------- Helpers ----------------

    private void validateUniqueEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
    }

    private void validateUniqueEmail(String email, Long excludeId) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                throw new IllegalArgumentException("Email already exists: " + email);
            }
        });
    }

    private void validateUniqueUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
    }

    private void validateUniqueUsername(String username, Long excludeId) {
        userRepository.findByEmailIgnoreCase(username).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                throw new IllegalArgumentException("Username already exists: " + username);
            }
        });
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            Role defaultRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new IllegalArgumentException("Default USER role not found"));
            return Set.of(defaultRole);
        }
        List<Role> roles = roleRepository.findAllByIdIn(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new IllegalArgumentException("One or more roles not found");
        }
        return new HashSet<>(roles);
    }

    private AppUserDTO convertToDTO(AppUser user) {
        AppUserDTO dto = new AppUserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());

        if (user.getRoles() != null) {
            Set<RoleDTO> roleDTOs = user.getRoles().stream()
                    .map(this::convertRoleToDTO)
                    .collect(Collectors.toSet());
            dto.setRoles(roleDTOs);
        }
        return dto;
    }
    public AppUser getUserEntityByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }

    public void saveUser(AppUser user) {
        userRepository.save(user);
    }

    @Transactional
    public AppUser createUserEntity(UserRequest request) {

        validateUniqueEmail(request.getEmail());
        validateUniqueUsername(request.getUsername());

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        AppUser user = new AppUser();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(request.isEnabled());

        user.setRoles(resolveRoles(request.getRoleIds()));

        return userRepository.save(user);
    }

    private RoleDTO convertRoleToDTO(Role role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());

        if (role.getPermissions() != null) {
            Set<PermissionDTO> perms = role.getPermissions().stream()
                    .map(p -> {
                        PermissionDTO pdto = new PermissionDTO();
                        pdto.setId(p.getId());
                        pdto.setResource(p.getResource());
                        pdto.setAction(p.getAction());
                        return pdto;
                    })
                    .collect(Collectors.toSet());
            dto.setPermissions(perms);
        }

        return dto;
    }

}
