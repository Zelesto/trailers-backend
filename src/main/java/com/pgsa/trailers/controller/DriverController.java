package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.DriverDTO;
import com.pgsa.trailers.dto.DriverRequest;
import com.pgsa.trailers.service.DriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
public class DriverController {

    // ============================================================
    // CONSTANTS FOR STATUS VALUES (from enum_master table)
    // ============================================================
    public static final String STATUS_ACTIVE = "ACTIVE";

    private final DriverService driverService;

    // ====== GET Endpoints ======
    
    @GetMapping
    public ResponseEntity<List<DriverDTO>> getAllDrivers() {
        log.info("GET /api/drivers");
        try {
            List<DriverDTO> drivers = driverService.getAllDrivers();
            return ResponseEntity.ok(drivers);
        } catch (Exception e) {
            log.error("Error fetching drivers: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ FIXED: Use String constant instead of enum
    @GetMapping("/active")
    public ResponseEntity<List<DriverDTO>> getActiveDrivers() {
        log.info("GET /api/drivers/active");
        try {
            List<DriverDTO> drivers = driverService.getDriversByStatus(STATUS_ACTIVE);
            return ResponseEntity.ok(drivers);
        } catch (Exception e) {
            log.error("Error fetching active drivers: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverDTO> getDriverById(@PathVariable Long id) {
        log.info("GET /api/drivers/{}", id);
        try {
            DriverDTO driver = driverService.getDriverById(id);
            return ResponseEntity.ok(driver);
        } catch (RuntimeException e) {
            log.error("Driver not found with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching driver by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ FIXED: Already using String - no changes needed
    @GetMapping("/status/{status}")
    public ResponseEntity<List<DriverDTO>> getDriversByStatus(@PathVariable String status) {
        log.info("GET /api/drivers/status/{}", status);
        List<DriverDTO> drivers = driverService.getDriversByStatus(status);
        return ResponseEntity.ok(drivers);
    }

    // ====== POST Endpoints ======
    
    @PostMapping
    public ResponseEntity<?> createDriver(@RequestBody DriverRequest request) {
        log.info("POST /api/drivers - Creating driver: {} {}", 
            request.getFirstName(), request.getLastName());
        log.info("📥 Received request - firstName: {}, lastName: {}, licenseNumber: {}, status: {}", 
            request.getFirstName(), request.getLastName(), 
            request.getLicenseNumber(), request.getStatus());
        
        try {
            DriverDTO created = driverService.createDriver(request);
            log.info("✅ Driver created successfully - ID: {}, name: {}", 
                created.getId(), created.getFirstName() + " " + created.getLastName());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            log.error("Error creating driver: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error creating driver: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to create driver: " + e.getMessage());
        }
    }

    // ====== PUT Endpoints ======
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDriver(@PathVariable Long id, @RequestBody DriverRequest request) {
        log.info("PUT /api/drivers/{} - Updating driver", id);
        log.info("📥 Received request - firstName: {}, lastName: {}, licenseNumber: {}, status: {}", 
            request.getFirstName(), request.getLastName(), 
            request.getLicenseNumber(), request.getStatus());
        
        try {
            DriverDTO updated = driverService.updateDriver(id, request);
            log.info("✅ Driver updated successfully - ID: {}, name: {}", 
                updated.getId(), updated.getFirstName() + " " + updated.getLastName());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            log.error("Error updating driver {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating driver {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to update driver: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/assign-vehicle/{vehicleId}")
    public ResponseEntity<?> assignVehicle(@PathVariable Long id, @PathVariable Long vehicleId) {
        log.info("PUT /api/drivers/{}/assign-vehicle/{}", id, vehicleId);
        try {
            driverService.assignVehicle(id, vehicleId);
            return ResponseEntity.ok().body("Vehicle assigned successfully");
        } catch (RuntimeException e) {
            log.error("Error assigning vehicle: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error assigning vehicle: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to assign vehicle: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/unassign-vehicle")
    public ResponseEntity<?> unassignVehicle(@PathVariable Long id) {
        log.info("PUT /api/drivers/{}/unassign-vehicle", id);
        try {
            driverService.unassignVehicle(id);
            return ResponseEntity.ok().body("Vehicle unassigned successfully");
        } catch (RuntimeException e) {
            log.error("Error unassigning vehicle: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error unassigning vehicle: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to unassign vehicle: " + e.getMessage());
        }
    }

    // ✅ FIXED: Use String directly instead of enum
    @PutMapping("/{id}/status/{status}")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @PathVariable String status) {
        log.info("PUT /api/drivers/{}/status/{}", id, status);
        try {
            // Pass the String directly - DriverService will handle it
            driverService.updateStatus(id, status.toUpperCase());
            return ResponseEntity.ok().body("Status updated successfully");
        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", status);
            return ResponseEntity.badRequest().body("Invalid status: " + status);
        } catch (RuntimeException e) {
            log.error("Error updating status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating status: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to update status: " + e.getMessage());
        }
    }

    // ====== DELETE Endpoints ======
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDriver(@PathVariable Long id) {
        log.info("DELETE /api/drivers/{} - Deleting driver", id);
        try {
            driverService.deleteDriver(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Driver not found with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting driver {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to delete driver: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/soft")
    public ResponseEntity<?> softDeleteDriver(@PathVariable Long id) {
        log.info("DELETE /api/drivers/{}/soft - Soft deleting driver", id);
        try {
            driverService.softDeleteDriver(id);
            return ResponseEntity.ok().body("Driver soft deleted successfully");
        } catch (RuntimeException e) {
            log.error("Driver not found with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error soft deleting driver {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to soft delete driver: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restoreDriver(@PathVariable Long id) {
        log.info("PUT /api/drivers/{}/restore - Restoring driver", id);
        try {
            driverService.restoreDriver(id);
            return ResponseEntity.ok().body("Driver restored successfully");
        } catch (RuntimeException e) {
            log.error("Driver not found with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error restoring driver {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to restore driver: " + e.getMessage());
        }
    }
}
