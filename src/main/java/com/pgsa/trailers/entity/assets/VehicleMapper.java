// src/main/java/com/pgsa/trailers/mapper/VehicleMapper.java
package com.pgsa.trailers.entity.assets;

import com.pgsa.trailers.dto.VehicleDTO;
import com.pgsa.trailers.entity.assets.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {
    
    public VehicleDTO toDto(Vehicle vehicle) {
        if (vehicle == null) return null;
        
        VehicleDTO dto = new VehicleDTO();
        dto.setId(vehicle.getId());
        dto.setRegistrationNumber(vehicle.getRegistrationNumber());
        dto.setVin(vehicle.getVin());
        dto.setMake(vehicle.getMake());
        dto.setModel(vehicle.getModel());
        dto.setYear(vehicle.getYear());
        // ✅ FIXED: Remove .name() - vehicleType is now a String
        dto.setVehicleType(vehicle.getVehicleType());
        dto.setFuelType(vehicle.getFuelType());
        // ✅ FIXED: Remove .name() - status is now a String
        dto.setStatus(vehicle.getStatus());
        dto.setCurrentMileage(vehicle.getCurrentMileage());
        dto.setCurrentOdometer(vehicle.getCurrentOdometer());
        
        // Fuel fields - using the correct getters
        dto.setCurrentFuelLevel(vehicle.getCurrentFuelLevel());
        dto.setFuelCapacity(vehicle.getFuelCapacity());
        dto.setFuelTankCount(vehicle.getFuelTankCount());
        dto.setFuelTankType(vehicle.getFuelTankType());
        
        dto.setAvgConsumption(vehicle.getAvgConsumption());
        dto.setLastServiceDate(vehicle.getLastServiceDate());
        dto.setLastServiceOdometer(vehicle.getLastServiceOdometer());
        dto.setNextServiceDue(vehicle.getNextServiceDue());
        dto.setNextServiceOdometer(vehicle.getNextServiceOdometer());
        dto.setServiceIntervalDays(vehicle.getServiceIntervalDays());
        dto.setServiceIntervalKm(vehicle.getServiceIntervalKm());
        dto.setInsurancePolicyNumber(vehicle.getInsurancePolicyNumber());
        dto.setInsuranceExpiry(vehicle.getInsuranceExpiry());
        dto.setRoadworthyExpiry(vehicle.getRoadworthyExpiry());
        dto.setFleetNumber(vehicle.getFleetNumber());
        dto.setGpsTrackerId(vehicle.getGpsTrackerId());
        dto.setMaintenanceStatus(vehicle.getMaintenanceStatus());
        dto.setIncidentsLogged(vehicle.getIncidentsLogged());
        dto.setNotes(vehicle.getNotes());
        dto.setCategory(vehicle.getCategory());
        dto.setIsActive(vehicle.getIsActive());
        dto.setVersion(vehicle.getVersion());
        dto.setCurrentValue(vehicle.getCurrentValue());
        dto.setPurchaseDate(vehicle.getPurchaseDate());
        dto.setPurchasePrice(vehicle.getPurchasePrice());
        dto.setMaintenanceCost(vehicle.getMaintenanceCost());
        dto.setLastMaintenanceDate(vehicle.getLastMaintenanceDate());
        dto.setNextMaintenanceDue(vehicle.getNextMaintenanceDue());
        dto.setFuelEfficiency(vehicle.getFuelEfficiency());
        dto.setInsuranceProvider(vehicle.getInsuranceProvider());
        dto.setInsuranceExpiryDate(vehicle.getInsuranceExpiryDate());
        
        if (vehicle.getAssignedDriver() != null) {
            dto.setAssignedDriverId(vehicle.getAssignedDriver().getId());
        }
        
        return dto;
    }
}
