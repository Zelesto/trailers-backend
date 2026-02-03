package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.AppUserDTO;
import com.pgsa.trailers.entity.security.DriverMapper;
import com.pgsa.trailers.dto.DriverDTO;
import com.pgsa.trailers.dto.DriverRequest;
import com.pgsa.trailers.dto.UserRequest;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.security.AppUser;
import com.pgsa.trailers.enums.DriverStatus;
import com.pgsa.trailers.repository.DriverRepository;
import com.pgsa.trailers.repository.RoleRepository;
import com.pgsa.trailers.service.security.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserService userService;
    private final RoleRepository roleRepository;   // inject RoleRepository
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Transactional
    public DriverDTO createDriver(DriverRequest request) {
        // Create UserRequest for AppUser
        UserRequest userRequest = new UserRequest();
        userRequest.setEmail(request.getEmail());
        userRequest.setUsername(request.getEmail());
        userRequest.setPassword(request.getPassword());
        userRequest.setEnabled(true);
        userRequest.setRoleIds(Set.of(driverRoleId()));

        AppUser appUser = userService.createUserEntity(userRequest);

        // Map DriverRequest to Driver entity
        Driver driver = new Driver();
        driver.setFirstName(request.getFirstName());
        driver.setLastName(request.getLastName());
        driver.setEmail(request.getEmail());
        driver.setPhoneNumber(request.getPhoneNumber());
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setLicenseType(request.getLicenseType());
        driver.setLicenseExpiry(request.getLicenseExpiry());
        driver.setHireDate(request.getHireDate());
        driver.setAppUser(appUser);

        if (request.getStatus() != null) {
            driver.setStatus(DriverStatus.valueOf(request.getStatus().toUpperCase()));
        } else {
            driver.setStatus(DriverStatus.ACTIVE);
        }

        Driver savedDriver = driverRepository.save(driver);

        return DriverMapper.toDTO(savedDriver);
    }



    private Long driverRoleId() {
        return roleRepository.findByName("DRIVER")
                .orElseThrow(() -> new RuntimeException("DRIVER role not found"))
                .getId();
    }

    @Transactional(readOnly = true)
    public DriverDTO getDriverById(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        return DriverMapper.toDTO(driver);
    }

    @Transactional(readOnly = true)
    public List<DriverDTO> getAllDrivers() {
        return driverRepository.findAll()
                .stream()
                .map(DriverMapper::toDTO)
                .toList();
    }

    @Transactional
    public void deleteDriver(Long id) {
        driverRepository.deleteById(id);
    }
}
