package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.assets.Driver;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    // Basic finders
    Optional<Driver> findByLicenseNumber(String licenseNumber);
    Optional<Driver> findByEmail(String email);
    Optional<Driver> findByPhoneNumber(String phoneNumber);
    Optional<Driver> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);
    
    // Status finders
    List<Driver> findByStatus(String status);
    List<Driver> findByStatusIn(List<String> statuses);
    
    // Active finders
    List<Driver> findByIsActiveTrue();
    List<Driver> findByIsActiveTrueAndStatus(String status);
    
    // License expiry
    List<Driver> findByLicenseExpiryBefore(LocalDate date);
    List<Driver> findByLicenseExpiryBetween(LocalDate start, LocalDate end);
    
    // Medical clearance
    List<Driver> findByNextMedicalDueBefore(LocalDate date);
    List<Driver> findByNextMedicalDueBetween(LocalDate start, LocalDate end);
    
    // Search
    @Query("SELECT d FROM Driver d WHERE " +
           "LOWER(d.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(d.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(d.licenseNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(d.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Driver> searchDrivers(@Param("searchTerm") String searchTerm);
    
    // By AppUser
    Optional<Driver> findByAppUserId(Long appUserId);
    
    // Counts
    long countByStatus(String status);
    long countByIsActiveTrue();
    
    // Available for assignment
   @Query("SELECT d FROM Driver d WHERE d.status = :status AND d.isActive = true AND d.assignedVehicleId IS NULL")
    List<Driver> findAvailableDrivers(@Param("status") String status);
}
