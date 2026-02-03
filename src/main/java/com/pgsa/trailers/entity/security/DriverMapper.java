package com.pgsa.trailers.entity.security;

import com.pgsa.trailers.dto.AppUserDTO;
import com.pgsa.trailers.dto.DriverDTO;
import com.pgsa.trailers.dto.RoleDTO;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.security.AppUser;
import com.pgsa.trailers.entity.security.Role;

import java.util.Set;
import java.util.stream.Collectors;

public class DriverMapper {

    public static DriverDTO toDTO(Driver driver) {
        if (driver == null) return null;

        DriverDTO dto = new DriverDTO();
        dto.setId(driver.getId());
        dto.setFirstName(driver.getFirstName());
        dto.setLastName(driver.getLastName());
        dto.setLicenseNumber(driver.getLicenseNumber());
        dto.setLicenseType(driver.getLicenseType());
        dto.setLicenseExpiry(driver.getLicenseExpiry());
        dto.setHireDate(driver.getHireDate());
        dto.setPhoneNumber(driver.getPhoneNumber());
        dto.setEmail(driver.getEmail());

        // Convert enum to string for DTO
        dto.setStatus(driver.getStatus() != null ? driver.getStatus().name() : null);

        // ✅ Map the linked AppUser to DTO
        dto.setAppUser(toAppUserDTO(driver.getAppUser()));

        return dto;
    }

    private static AppUserDTO toAppUserDTO(AppUser appUser) {
        if (appUser == null) return null;

        AppUserDTO dto = new AppUserDTO();
        dto.setId(appUser.getId());
        dto.setUsername(appUser.getUsername());
        dto.setEmail(appUser.getEmail());
        dto.setEnabled(appUser.isEnabled());

        // Convert roles from entity to DTO
        dto.setRoles(toRoleDTOSet(appUser.getRoles()));

        return dto;
    }

    private static Set<RoleDTO> toRoleDTOSet(Set<Role> roles) {
        if (roles == null) return Set.of();
        return roles.stream()
                .map(role -> {
                    RoleDTO dto = new RoleDTO();
                    dto.setId(role.getId());
                    dto.setName(role.getName());
                    dto.setDescription(role.getDescription());
                    return dto;
                })
                .collect(Collectors.toSet());
    }
}
