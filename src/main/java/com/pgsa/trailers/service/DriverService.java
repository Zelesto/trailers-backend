package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.DriverDTO;
import com.pgsa.trailers.dto.DriverRequest;
import com.pgsa.trailers.dto.UserRequest;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.security.AppUser;
import com.pgsa.trailers.repository.DriverRepository;
import com.pgsa.trailers.repository.AppUserRepository;
import com.pgsa.trailers.service.security.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import java.util.HashSet; 
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverService {

    // ============================================================
    // CONSTANTS FOR STATUS VALUES (from enum_master table)
    // ============================================================
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_ON_LEAVE = "ON_LEAVE";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_ON_TRIP = "ON_TRIP";

    private final DriverRepository driverRepository;
    private final AppUserRepository appUserRepository;
    private final UserService userService;

    // ====== CREATE ======
    
    @Transactional
    public DriverDTO createDriver(DriverRequest request) {
        log.info("Creating driver: {} {}", request.getFirstName(), request.getLastName());
        
        // Validate required fields
        validateDriverRequest(request);
        
        // Check for duplicate license number
        if (driverRepository.findByLicenseNumber(request.getLicenseNumber()).isPresent()) {
            throw new RuntimeException("Driver with license number " + request.getLicenseNumber() + " already exists");
        }
        
        // Get or create AppUser
        AppUser appUser;
        if (request.getAppUserId() != null) {
            appUser = appUserRepository.findById(request.getAppUserId())
                    .orElseThrow(() -> new RuntimeException("AppUser not found with ID: " + request.getAppUserId()));
        } else {
            // Create new AppUser if not provided
            appUser = createAppUser(request);
        }
        
        Driver driver = new Driver();
        mapRequestToEntity(request, driver);
        driver.setAppUser(appUser);
        
        // Set defaults
        if (driver.getStatus() == null) {
            driver.setStatus(STATUS_ACTIVE);
        }
        
        Driver saved = driverRepository.save(driver);
        log.info("✅ Successfully created driver with ID: {}", saved.getId());
        return convertToDTO(saved);
    }

    // ====== READ ======
    
    public List<DriverDTO> getAllDrivers() {
        log.debug("Fetching all drivers");
        return driverRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public DriverDTO getDriverById(Long id) {
        log.debug("Fetching driver by ID: {}", id);
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + id));
        return convertToDTO(driver);
    }

    public List<DriverDTO> getDriversByStatus(String status) {
        return driverRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ====== UPDATE ======
    
    @Transactional
    public DriverDTO updateDriver(Long id, DriverRequest request) {
        log.info("Updating driver ID: {}", id);
        
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + id));
        
        log.info("Current driver - ID: {}, Name: {}, License: {}, AppUser ID: {}", 
            driver.getId(), driver.getFullName(), driver.getLicenseNumber(), 
            driver.getAppUser() != null ? driver.getAppUser().getId() : "null");
        
        // Update basic fields
        if (request.getFirstName() != null) {
            driver.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            driver.setLastName(request.getLastName().trim());
        }
        if (request.getEmail() != null) {
            driver.setEmail(request.getEmail().trim());
        }
        if (request.getPhoneNumber() != null) {
            driver.setPhoneNumber(request.getPhoneNumber().trim());
        }
        if (request.getLicenseNumber() != null) {
            if (!driver.getLicenseNumber().equals(request.getLicenseNumber())) {
                if (driverRepository.findByLicenseNumber(request.getLicenseNumber()).isPresent()) {
                    throw new RuntimeException("License number " + request.getLicenseNumber() + " already exists");
                }
                driver.setLicenseNumber(request.getLicenseNumber().trim());
            }
        }
        if (request.getLicenseType() != null) {
            driver.setLicenseType(request.getLicenseType().trim());
        }
        if (request.getLicenseExpiry() != null) {
            driver.setLicenseExpiry(request.getLicenseExpiry());
        }
        if (request.getHireDate() != null) {
            driver.setHireDate(request.getHireDate());
        }
        if (request.getStatus() != null) {
            driver.setStatus(request.getStatus().toUpperCase());
        }
        if (request.getEmploymentType() != null) {
            driver.setEmploymentType(request.getEmploymentType().trim());
        }
        if (request.getShiftPattern() != null) {
            driver.setShiftPattern(request.getShiftPattern().trim());
        }
        if (request.getTrainingCompleted() != null) {
            driver.setTrainingCompleted(request.getTrainingCompleted());
        }
        if (request.getMedicalClearanceDate() != null) {
            driver.setMedicalClearanceDate(request.getMedicalClearanceDate());
        }
        if (request.getNextMedicalDue() != null) {
            driver.setNextMedicalDue(request.getNextMedicalDue());
        }
        if (request.getNotes() != null) {
            driver.setNotes(request.getNotes().trim());
        }
        
        // Handle AppUser update
        if (request.getAppUserId() != null) {
            AppUser appUser = appUserRepository.findById(request.getAppUserId())
                    .orElseThrow(() -> new RuntimeException("AppUser not found with ID: " + request.getAppUserId()));
            driver.setAppUser(appUser);
            log.info("Updated AppUser to ID: {}", appUser.getId());
        } else {
            if (request.getEmail() != null && driver.getAppUser() != null) {
                AppUser appUser = driver.getAppUser();
                if (!appUser.getEmail().equals(request.getEmail())) {
                    appUser.setEmail(request.getEmail());
                    appUserRepository.save(appUser);
                    log.info("Updated AppUser email to: {}", request.getEmail());
                }
            }
            log.info("Keeping existing AppUser ID: {}", 
                driver.getAppUser() != null ? driver.getAppUser().getId() : "null");
        }
        
        driver.setUpdatedAt(LocalDateTime.now());
        
        Driver saved = driverRepository.save(driver);
        log.info("✅ Successfully updated driver ID: {}, Name: {}", saved.getId(), saved.getFullName());
        return convertToDTO(saved);
    }

    // ====== DELETE ======
    
    @Transactional
    public void deleteDriver(Long id) {
        log.info("Deleting driver ID: {}", id);
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + id));
        driverRepository.delete(driver);
        log.info("✅ Successfully deleted driver ID: {}", id);
    }

    @Transactional
    public void softDeleteDriver(Long id) {
        log.info("Soft deleting driver ID: {}", id);
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + id));
        driver.setIsActive(false);
        driver.setStatus(STATUS_INACTIVE);
        driver.setUpdatedAt(LocalDateTime.now());
        driverRepository.save(driver);
        log.info("✅ Successfully soft deleted driver ID: {}", id);
    }

    @Transactional
    public void restoreDriver(Long id) {
        log.info("Restoring driver ID: {}", id);
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + id));
        driver.setIsActive(true);
        driver.setStatus(STATUS_ACTIVE);
        driver.setUpdatedAt(LocalDateTime.now());
        driverRepository.save(driver);
        log.info("✅ Successfully restored driver ID: {}", id);
    }

    // ====== BUSINESS OPERATIONS ======
    
    @Transactional
    public void assignVehicle(Long driverId, Long vehicleId) {
        log.info("Assigning vehicle {} to driver {}", vehicleId, driverId);
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + driverId));
        driver.setAssignedVehicleId(vehicleId);
        driver.setUpdatedAt(LocalDateTime.now());
        driverRepository.save(driver);
        log.info("✅ Vehicle {} assigned to driver {}", vehicleId, driverId);
    }

    @Transactional
    public void unassignVehicle(Long driverId) {
        log.info("Unassigning vehicle from driver {}", driverId);
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + driverId));
        driver.setAssignedVehicleId(null);
        driver.setUpdatedAt(LocalDateTime.now());
        driverRepository.save(driver);
        log.info("✅ Vehicle unassigned from driver {}", driverId);
    }

    @Transactional
    public void updateStatus(Long driverId, String status) {
        log.info("Updating driver {} status to {}", driverId, status);
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        
        String statusUpper = status.toUpperCase();
        driver.setStatus(statusUpper);
        driver.setUpdatedAt(LocalDateTime.now());
        driverRepository.save(driver);
        log.info("✅ Driver {} status updated to {}", driverId, statusUpper);
    }

    // ====== HELPER METHODS ======
    
    private void validateDriverRequest(DriverRequest request) {
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new RuntimeException("First name is required");
        }
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new RuntimeException("Last name is required");
        }
        if (request.getLicenseNumber() == null || request.getLicenseNumber().trim().isEmpty()) {
            throw new RuntimeException("License number is required");
        }
        if (request.getAppUserId() == null && (request.getPassword() == null || request.getPassword().isEmpty())) {
            throw new RuntimeException("Either appUserId or password is required");
        }
    }

    private void mapRequestToEntity(DriverRequest request, Driver driver) {
        if (request.getFirstName() != null) {
            driver.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            driver.setLastName(request.getLastName().trim());
        }
        if (request.getLicenseNumber() != null) {
            driver.setLicenseNumber(request.getLicenseNumber().trim());
        }
        if (request.getLicenseType() != null) {
            driver.setLicenseType(request.getLicenseType().trim());
        }
        if (request.getLicenseExpiry() != null) {
            driver.setLicenseExpiry(request.getLicenseExpiry());
        }
        if (request.getHireDate() != null) {
            driver.setHireDate(request.getHireDate());
        }
        if (request.getPhoneNumber() != null) {
            driver.setPhoneNumber(request.getPhoneNumber().trim());
        }
        if (request.getEmail() != null) {
            driver.setEmail(request.getEmail().trim());
        }
        if (request.getStatus() != null) {
            driver.setStatus(request.getStatus().toUpperCase());
        }
        if (request.getEmploymentType() != null) {
            driver.setEmploymentType(request.getEmploymentType().trim());
        }
        if (request.getShiftPattern() != null) {
            driver.setShiftPattern(request.getShiftPattern().trim());
        }
        if (request.getTrainingCompleted() != null) {
            driver.setTrainingCompleted(request.getTrainingCompleted());
        }
        if (request.getMedicalClearanceDate() != null) {
            driver.setMedicalClearanceDate(request.getMedicalClearanceDate());
        }
        if (request.getNextMedicalDue() != null) {
            driver.setNextMedicalDue(request.getNextMedicalDue());
        }
        if (request.getNotes() != null) {
            driver.setNotes(request.getNotes().trim());
        }
    }

    private AppUser createAppUser(DriverRequest request) {
        UserRequest userRequest = new UserRequest();
        userRequest.setUsername(request.getEmail() != null ? request.getEmail() : request.getLicenseNumber());
        userRequest.setEmail(request.getEmail());
        userRequest.setPassword(request.getPassword());
        userRequest.setEnabled(true);
        
        Set<Long> roleIds = new HashSet<>();
        roleIds.add(2L);
        userRequest.setRoleIds(roleIds);
        
        return userService.createUserEntity(userRequest);
    }

    private DriverDTO convertToDTO(Driver driver) {
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
        dto.setStatus(driver.getStatus());
        dto.setTerminationDate(driver.getTerminationDate());
        dto.setTerminationReason(driver.getTerminationReason());
        dto.setEmploymentType(driver.getEmploymentType());
        dto.setShiftPattern(driver.getShiftPattern());
        dto.setAssignedVehicleId(driver.getAssignedVehicleId());
        dto.setTrainingCompleted(driver.getTrainingCompleted());
        dto.setMedicalClearanceDate(driver.getMedicalClearanceDate());
        dto.setNextMedicalDue(driver.getNextMedicalDue());
        dto.setIncidentsLogged(driver.getIncidentsLogged());
        dto.setTotalTrips(driver.getTotalTrips());
        dto.setTotalKmTravelled(driver.getTotalKmTravelled());
        dto.setTotalHoursActive(driver.getTotalHoursActive());
        dto.setPerformanceScore(driver.getPerformanceScore());
        dto.setNotes(driver.getNotes());
        dto.setIsActive(driver.getIsActive());
        dto.setVersion(driver.getVersion());
        dto.setAppUserId(driver.getAppUser() != null ? driver.getAppUser().getId() : null);
        return dto;
    }
}
