package com.pgsa.trailers.controller;

import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    /**
     * Get all vehicles
     */
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

    /**
     * Get vehicle by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getVehicleById(@PathVariable Long id) {
        log.info("GET /api/vehicles/{}", id);
        try {
            Vehicle vehicle = vehicleService.getVehicleById(id);
            return ResponseEntity.ok(vehicle);
        } catch (Exception e) {
            log.error("Error fetching vehicle by ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get vehicle by registration number
     */
    @GetMapping("/registration/{registrationNumber}")
    public ResponseEntity<Vehicle> getVehicleByRegistration(@PathVariable String registrationNumber) {
        log.info("GET /api/vehicles/registration/{}", registrationNumber);
        try {
            Vehicle vehicle = vehicleService.getVehicleByRegistration(registrationNumber);
            return ResponseEntity.ok(vehicle);
        } catch (Exception e) {
            log.error("Error fetching vehicle by registration {}: {}", registrationNumber, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create new vehicle
     */
    @PostMapping
    public ResponseEntity<Vehicle> createVehicle(@RequestBody Vehicle vehicle) {
        log.info("POST /api/vehicles - Creating vehicle: {}", vehicle.getRegistrationNumber());
        try {
            Vehicle createdVehicle = vehicleService.createVehicle(vehicle);
            return ResponseEntity.ok(createdVehicle);
        } catch (Exception e) {
            log.error("Error creating vehicle: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update vehicle
     */
    @PutMapping("/{id}")
    public ResponseEntity<Vehicle> updateVehicle(@PathVariable Long id, @RequestBody Vehicle vehicle) {
        log.info("PUT /api/vehicles/{} - Updating vehicle", id);
        try {
            Vehicle updatedVehicle = vehicleService.updateVehicle(id, vehicle);
            return ResponseEntity.ok(updatedVehicle);
        } catch (Exception e) {
            log.error("Error updating vehicle {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete vehicle
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        log.info("DELETE /api/vehicles/{} - Deleting vehicle", id);
        try {
            vehicleService.deleteVehicle(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting vehicle {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get vehicles by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Vehicle>> getVehiclesByStatus(@PathVariable String status) {
        log.info("GET /api/vehicles/status/{}", status);
        try {
            List<Vehicle> vehicles = vehicleService.getVehiclesByStatus(status);
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error fetching vehicles by status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get active vehicles
     */
    @GetMapping("/active")
    public ResponseEntity<List<Vehicle>> getActiveVehicles() {
        log.info("GET /api/vehicles/active");
        try {
            List<Vehicle> vehicles = vehicleService.getActiveVehicles();
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error fetching active vehicles: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}