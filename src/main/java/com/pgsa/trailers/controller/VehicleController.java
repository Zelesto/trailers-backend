package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.CertificateRequest;
import com.pgsa.trailers.dto.MaintenanceRecordRequest;
import com.pgsa.trailers.dto.MaintenanceRecordResponse;
import com.pgsa.trailers.dto.VehicleCertificateDTO;
import com.pgsa.trailers.dto.VehicleDTO;
import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.entity.vehicle.Certificate;
import com.pgsa.trailers.entity.vehicle.MaintenanceRecord;
import com.pgsa.trailers.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    // ============================================================
    // CONSTANTS FOR STATUS VALUES (from enum_master table)
    // ============================================================
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_IN_TRIP = "IN_TRIP";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_MAINTENANCE = "MAINTENANCE";
    public static final String STATUS_OUT_OF_SERVICE = "OUT_OF_SERVICE";

    // List of all valid statuses for validation
    private static final List<String> VALID_STATUSES = List.of(
        STATUS_AVAILABLE, STATUS_ASSIGNED, STATUS_IN_TRIP,
        STATUS_ACTIVE, STATUS_INACTIVE, STATUS_MAINTENANCE,
        STATUS_OUT_OF_SERVICE
    );

    // ====== GET Endpoints ======
    
    @GetMapping
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        log.info("GET /api/vehicles");
        try {
            List<Vehicle> vehicles = vehicleService.getAllVehicles();
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error fetching vehicles: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/active")
    public ResponseEntity<List<Vehicle>> getActiveVehicles() {
        log.info("GET /api/vehicles/active");
        try {
            List<Vehicle> vehicles = vehicleService.getAllActiveVehicles();
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error fetching active vehicles: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getVehicleById(@PathVariable Long id) {
        log.info("GET /api/vehicles/{}", id);
        try {
            Vehicle vehicle = vehicleService.getVehicleById(id);
            return ResponseEntity.ok(vehicle);
        } catch (RuntimeException e) {
            log.error("Vehicle not found with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching vehicle by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/maintenance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
    public ResponseEntity<?> addMaintenanceRecord(@RequestBody MaintenanceRecordRequest request) {
        log.info("📋 Adding maintenance record for vehicle: {}", request.getVehicleId());
        try {
            MaintenanceRecord record = vehicleService.addMaintenanceRecord(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Maintenance record added successfully",
                "record", record
            ));
        } catch (RuntimeException e) {
            log.error("Error adding maintenance record: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error adding maintenance record: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Failed to add maintenance record: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/maintenance/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
    public ResponseEntity<?> updateMaintenanceRecord(
            @PathVariable Long id,
            @RequestBody MaintenanceRecordRequest request
    ) {
        log.info("📋 Updating maintenance record: {}", id);
        try {
            MaintenanceRecord updated = vehicleService.updateMaintenanceRecord(id, request);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Maintenance record updated successfully",
                "record", updated
            ));
        } catch (Exception e) {
            log.error("Error updating maintenance record: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Failed to update maintenance record: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/maintenance/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
    public ResponseEntity<?> deleteMaintenanceRecord(@PathVariable Long id) {
        log.info("🗑️ Deleting maintenance record: {}", id);
        try {
            vehicleService.deleteMaintenanceRecord(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Maintenance record deleted successfully"
            ));
        } catch (Exception e) {
            log.error("Error deleting maintenance record: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Failed to delete maintenance record: " + e.getMessage()
            ));
        }
    }
    
    @PutMapping("/{id}/fuel-level")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
    public ResponseEntity<?> updateFuelLevel(
            @PathVariable Long id,
            @RequestBody Map<String, Double> request
    ) {
        log.info("⛽ Updating fuel level for vehicle: {}", id);
        try {
            Double fuelLevel = request.get("fuelLevel");
            if (fuelLevel == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "fuelLevel is required"
                ));
            }
            
            Vehicle vehicle = vehicleService.getVehicleById(id);
            vehicle.setCurrentFuelLevel(fuelLevel);
            vehicle.setLastFuelUpdate(LocalDateTime.now());
            
            Vehicle saved = vehicleService.updateVehicle(id, VehicleDTO.fromEntity(vehicle));
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Fuel level updated successfully",
                "vehicle", VehicleDTO.fromEntity(saved)
            ));
        } catch (Exception e) {
            log.error("Error updating fuel level: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Failed to update fuel level: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/{id}/certificates")
    public ResponseEntity<?> getCertificates(@PathVariable Long id) {
        log.info("GET /api/vehicles/{}/certificates", id);
        try {
            List<Certificate> certificates = vehicleService.getCertificatesByVehicleId(id);
            return ResponseEntity.ok(certificates != null ? certificates : Collections.emptyList());
        } catch (Exception e) {
            log.warn("Certificate endpoint not fully implemented, returning mock data");
            return ResponseEntity.ok(getMockCertificates(id));
        }
    }

    @GetMapping("/{id}/maintenance")
    public ResponseEntity<?> getMaintenanceSchedule(@PathVariable Long id) {
        log.info("GET /api/vehicles/{}/maintenance", id);
        try {
            List<MaintenanceRecord> records = vehicleService.getMaintenanceRecordsByVehicleId(id);
            return ResponseEntity.ok(records != null ? records : Collections.emptyList());
        } catch (Exception e) {
            log.warn("Maintenance endpoint not fully implemented, returning mock data");
            return ResponseEntity.ok(getMockMaintenanceRecords(id));
        }
    }

    @PostMapping("/{id}/fuel/reset")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
    public ResponseEntity<?> resetFuelToFull(
            @PathVariable Long id,
            @RequestParam(required = false) Integer tankNumber
    ) {
        log.info("⛽ Resetting fuel to full for vehicle: {}, tank: {}", id, tankNumber);
        try {
            VehicleDTO updated = vehicleService.resetFuelToFull(id, tankNumber);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Fuel reset to full successfully",
                "vehicle", updated
            ));
        } catch (RuntimeException e) {
            log.error("Error resetting fuel: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error resetting fuel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Failed to reset fuel: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/certificates")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
    public ResponseEntity<?> addCertificate(
            @PathVariable Long id,
            @RequestBody CertificateRequest request
    ) {
        log.info("📄 Adding certificate to vehicle: {}", id);
        try {
            VehicleCertificateDTO certificate = vehicleService.addCertificate(id, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Certificate added successfully",
                "certificate", certificate
            ));
        } catch (RuntimeException e) {
            log.error("Error adding certificate: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error adding certificate: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Failed to add certificate: " + e.getMessage()
            ));
        }
    }

    // ====== MOCK DATA METHODS ======
    
    private List<Certificate> getMockCertificates(Long vehicleId) {
        return Collections.emptyList();
    }

    private List<MaintenanceRecord> getMockMaintenanceRecords(Long vehicleId) {
        return Collections.emptyList();
    }

    @GetMapping("/registration/{registrationNumber}")
    public ResponseEntity<Vehicle> getVehicleByRegistration(@PathVariable String registrationNumber) {
        log.info("GET /api/vehicles/registration/{}", registrationNumber);
        try {
            Vehicle vehicle = vehicleService.getVehicleByRegistration(registrationNumber);
            return ResponseEntity.ok(vehicle);
        } catch (RuntimeException e) {
            log.error("Vehicle not found with registration {}: {}", registrationNumber, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching vehicle by registration {}: {}", registrationNumber, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ============================================================
    // STATUS ENDPOINTS - FIXED to use String
    // ============================================================
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Vehicle>> getVehiclesByStatus(@PathVariable String status) {
        log.info("GET /api/vehicles/status/{}", status);
        try {
            // Validate status is valid
            String statusUpper = status.toUpperCase();
            if (!VALID_STATUSES.contains(statusUpper)) {
                log.warn("Invalid status: {}", status);
                return ResponseEntity.badRequest().build();
            }
            
            List<Vehicle> vehicles = vehicleService.getVehiclesByStatus(statusUpper);
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error fetching vehicles by status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/available")
    public ResponseEntity<List<Vehicle>> getAvailableVehicles() {
        log.info("GET /api/vehicles/available");
        try {
            List<Vehicle> vehicles = vehicleService.getAvailableVehicles();
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error fetching available vehicles: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Vehicle>> searchVehicles(@RequestParam String term) {
        log.info("GET /api/vehicles/search?term={}", term);
        try {
            List<Vehicle> vehicles = vehicleService.searchVehicles(term);
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error searching vehicles: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<Vehicle>> getVehiclesByDriver(@PathVariable Long driverId) {
        log.info("GET /api/vehicles/driver/{}", driverId);
        try {
            List<Vehicle> vehicles = vehicleService.getVehiclesByDriver(driverId);
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error fetching vehicles for driver {}: {}", driverId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ====== POST Endpoints ======
    
    @PostMapping
    public ResponseEntity<?> createVehicle(@RequestBody VehicleDTO vehicleDTO) {
        log.info("POST /api/vehicles - Creating vehicle: {}", vehicleDTO.getRegistrationNumber());
        try {
            if (vehicleDTO.getRegistrationNumber() == null || vehicleDTO.getRegistrationNumber().isEmpty()) {
                return ResponseEntity.badRequest().body("Registration number is required");
            }
            
            Vehicle createdVehicle = vehicleService.createVehicleFromDTO(vehicleDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdVehicle);
        } catch (RuntimeException e) {
            log.error("Error creating vehicle: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating vehicle: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to create vehicle: " + e.getMessage());
        }
    }

    // ====== PUT Endpoints ======
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateVehicle(@PathVariable Long id, @RequestBody VehicleDTO vehicleDTO) {
        log.info("PUT /api/vehicles/{} - Updating vehicle", id);
        try {
            Vehicle updatedVehicle = vehicleService.updateVehicle(id, vehicleDTO);
            return ResponseEntity.ok(updatedVehicle);
        } catch (RuntimeException e) {
            log.error("Error updating vehicle {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating vehicle {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to update vehicle: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/assign-driver/{driverId}")
    public ResponseEntity<?> assignDriver(@PathVariable Long id, @PathVariable Long driverId) {
        log.info("PUT /api/vehicles/{}/assign-driver/{}", id, driverId);
        try {
            vehicleService.assignDriverToVehicle(id, driverId);
            return ResponseEntity.ok().body("Driver assigned successfully");
        } catch (RuntimeException e) {
            log.error("Error assigning driver: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error assigning driver: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to assign driver: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/unassign-driver")
    public ResponseEntity<?> unassignDriver(@PathVariable Long id) {
        log.info("PUT /api/vehicles/{}/unassign-driver", id);
        try {
            vehicleService.unassignDriverFromVehicle(id);
            return ResponseEntity.ok().body("Driver unassigned successfully");
        } catch (RuntimeException e) {
            log.error("Error unassigning driver: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error unassigning driver: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to unassign driver: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/odometer")
    public ResponseEntity<?> updateOdometer(@PathVariable Long id, @RequestParam BigDecimal odometer) {
        log.info("PUT /api/vehicles/{}/odometer?odometer={}", id, odometer);
        try {
            vehicleService.updateOdometer(id, odometer);
            return ResponseEntity.ok().body("Odometer updated successfully");
        } catch (RuntimeException e) {
            log.error("Error updating odometer: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating odometer: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to update odometer: " + e.getMessage());
        }
    }

    // ====== DELETE Endpoints ======
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id) {
        log.info("DELETE /api/vehicles/{} - Deleting vehicle", id);
        try {
            vehicleService.deleteVehicle(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Vehicle not found with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting vehicle {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to delete vehicle: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/soft")
    public ResponseEntity<?> softDeleteVehicle(@PathVariable Long id) {
        log.info("DELETE /api/vehicles/{}/soft - Soft deleting vehicle", id);
        try {
            vehicleService.softDeleteVehicle(id);
            return ResponseEntity.ok().body("Vehicle soft deleted successfully");
        } catch (RuntimeException e) {
            log.error("Vehicle not found with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error soft deleting vehicle {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to soft delete vehicle: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restoreVehicle(@PathVariable Long id) {
        log.info("PUT /api/vehicles/{}/restore - Restoring vehicle", id);
        try {
            vehicleService.restoreVehicle(id);
            return ResponseEntity.ok().body("Vehicle restored successfully");
        } catch (RuntimeException e) {
            log.error("Vehicle not found with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error restoring vehicle {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to restore vehicle: " + e.getMessage());
        }
    }
}
