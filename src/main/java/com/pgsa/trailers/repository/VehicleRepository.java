package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.assets.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // ====== Basic Finders ======
    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);
    Optional<Vehicle> findByRegistrationNumberIgnoreCase(String registrationNumber);
    Optional<Vehicle> findByVin(String vin);
    Optional<Vehicle> findByVinIgnoreCase(String vin);
    Optional<Vehicle> findByFleetNumber(String fleetNumber);
    
    // Active status with finders
    Optional<Vehicle> findByIdAndIsActiveTrue(Long id);
    List<Vehicle> findByIsActiveTrue();

    // ====== Status Finders ======
    List<Vehicle> findByStatus(String status);
    List<Vehicle> findByStatusIn(List<String> statuses);

    // ====== Vehicle Type ======
    List<Vehicle> findByVehicleType(String vehicleType);

    // ====== Make/Model ======
    List<Vehicle> findByMakeContainingIgnoreCase(String make);
    List<Vehicle> findByModelContainingIgnoreCase(String model);

    // ====== Search ======
    @Query("SELECT v FROM Vehicle v WHERE " +
           "LOWER(v.registrationNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(v.make) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(v.model) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(v.vin) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(v.fleetNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Vehicle> searchVehicles(@Param("searchTerm") String searchTerm);

    // ====== Driver Related ======
    List<Vehicle> findByAssignedDriverIsNotNull();
    List<Vehicle> findByAssignedDriverIsNull();
    List<Vehicle> findByAssignedDriverIsNullAndStatusIn(List<String> statuses);
    List<Vehicle> findByAssignedDriverId(Long driverId);

    // ====== Maintenance ======
    List<Vehicle> findByMaintenanceStatus(String maintenanceStatus);

    // ====== Service Related ======
    List<Vehicle> findByNextServiceDueBetween(LocalDate start, LocalDate end);
    List<Vehicle> findByNextServiceDueBefore(LocalDate date);
    List<Vehicle> findByInsuranceExpiryBefore(LocalDate date);
    List<Vehicle> findByRoadworthyExpiryBefore(LocalDate date);

    // ====== Year and Fuel ======
    List<Vehicle> findByYearBetween(Integer startYear, Integer endYear);
    List<Vehicle> findByFuelType(String fuelType);

    // ====== Counts ======
    long countByStatus(String status);
    long countByIsActiveTrue();
}
