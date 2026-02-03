package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    /**
     * Get all vehicles
     */
    public List<Vehicle> getAllVehicles() {
        log.debug("Fetching all vehicles");
        return vehicleRepository.findAll();
    }

    /**
     * Get vehicle by ID
     */
    public Vehicle getVehicleById(Long id) {
        log.debug("Fetching vehicle by ID: {}", id);
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with ID: " + id));
    }

    /**
     * Get vehicle by registration number
     */
    public Vehicle getVehicleByRegistration(String registrationNumber) {
        log.debug("Fetching vehicle by registration: {}", registrationNumber);
        return vehicleRepository.findByRegistrationNumber(registrationNumber)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with registration: " + registrationNumber));
    }

    /**
     * Create a new vehicle
     */
    @Transactional
    public Vehicle createVehicle(Vehicle vehicle) {
        log.info("Creating vehicle with registration: {}", vehicle.getRegistrationNumber());

        // Check if vehicle with same registration already exists
        if (vehicleRepository.findByRegistrationNumber(vehicle.getRegistrationNumber()).isPresent()) {
            throw new RuntimeException("Vehicle with registration " + vehicle.getRegistrationNumber() + " already exists");
        }

        return vehicleRepository.save(vehicle);
    }

    /**
     * Update vehicle
     */
    @Transactional
    public Vehicle updateVehicle(Long id, Vehicle vehicleDetails) {
        log.info("Updating vehicle ID: {}", id);

        Vehicle vehicle = getVehicleById(id);

        // Update basic fields
        vehicle.setRegistrationNumber(vehicleDetails.getRegistrationNumber());
        vehicle.setVin(vehicleDetails.getVin());
        vehicle.setMake(vehicleDetails.getMake());
        vehicle.setModel(vehicleDetails.getModel());
        vehicle.setYear(vehicleDetails.getYear());
        vehicle.setFuelType(vehicleDetails.getFuelType());
        vehicle.setCurrentMileage(vehicleDetails.getCurrentMileage());
        vehicle.setAvgConsumption(vehicleDetails.getAvgConsumption());
        vehicle.setCurrentOdometer(vehicleDetails.getCurrentOdometer());
        vehicle.setStatus(vehicleDetails.getStatus());

        // Update service-related fields
        vehicle.setLastServiceDate(vehicleDetails.getLastServiceDate());
        vehicle.setLastServiceOdometer(vehicleDetails.getLastServiceOdometer());
        vehicle.setServiceIntervalDays(vehicleDetails.getServiceIntervalDays());
        vehicle.setServiceIntervalKm(vehicleDetails.getServiceIntervalKm());
        vehicle.setNextServiceDue(vehicleDetails.getNextServiceDue());
        vehicle.setNextServiceOdometer(vehicleDetails.getNextServiceOdometer());
        vehicle.setMaintenanceStatus(vehicleDetails.getMaintenanceStatus());

        // Update insurance and roadworthy fields
        vehicle.setInsurancePolicyNumber(vehicleDetails.getInsurancePolicyNumber());
        vehicle.setInsuranceExpiry(vehicleDetails.getInsuranceExpiry());
        vehicle.setRoadworthyExpiry(vehicleDetails.getRoadworthyExpiry());

        // Update other fields
        vehicle.setFleetNumber(vehicleDetails.getFleetNumber());
        vehicle.setAssignedDriver(vehicleDetails.getAssignedDriver());
        vehicle.setGpsTrackerId(vehicleDetails.getGpsTrackerId());
        vehicle.setIncidentsLogged(vehicleDetails.getIncidentsLogged());
        vehicle.setNotes(vehicleDetails.getNotes());
        vehicle.setAuditTrail(vehicleDetails.getAuditTrail());

        return vehicleRepository.save(vehicle);
    }

    /**
     * Delete vehicle
     */
    @Transactional
    public void deleteVehicle(Long id) {
        log.info("Deleting vehicle ID: {}", id);
        Vehicle vehicle = getVehicleById(id);
        vehicleRepository.delete(vehicle);
    }

    /**
     * Get vehicles by status
     */
    public List<Vehicle> getVehiclesByStatus(String status) {
        log.debug("Fetching vehicles by status: {}", status);
        return vehicleRepository.findByStatus(status);
    }

    /**
     * Get active vehicles
     */
    public List<Vehicle> getActiveVehicles() {
        log.debug("Fetching active vehicles");
        return vehicleRepository.findByStatusIn(List.of("ACTIVE", "AVAILABLE"));
    }
}