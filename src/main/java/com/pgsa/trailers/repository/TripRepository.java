package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    // Basic CRUD methods are inherited from JpaRepository

    // Find by status
    List<Trip> findByStatus(String status);

    // Find by driver ID (using relationship path)
    List<Trip> findByDriver_Id(Long driverId);

    // Find by vehicle ID (using relationship path)
    List<Trip> findByVehicle_Id(Long vehicleId);

    // Find by load ID (using relationship path)
    List<Trip> findByLoad_Id(Long loadId);

    // Combined filter method - you need to implement this
    @Query("SELECT t FROM Trip t WHERE " +
            "(:driverId IS NULL OR t.driver.id = :driverId) AND " +
            "(:vehicleId IS NULL OR t.vehicle.id = :vehicleId) AND " +
            "(:status IS NULL OR t.status = :status)")
    Page<Trip> findByDriverIdAndVehicleIdAndStatus(
            @Param("driverId") Long driverId,
            @Param("vehicleId") Long vehicleId,
            @Param("status") String status,
            Pageable pageable);

    // Alternative simpler filter methods
    List<Trip> findByDriver_IdAndStatus(Long driverId, String status);

    List<Trip> findByVehicle_IdAndStatus(Long vehicleId, String status);

    // Find by trip number
    Trip findByTripNumber(String tripNumber);

    // Check if trip number exists
    boolean existsByTripNumber(String tripNumber);

    // Find trips by date range
    @Query("SELECT t FROM Trip t WHERE t.plannedStartDate BETWEEN :startDate AND :endDate")
    List<Trip> findTripsBetweenDates(@Param("startDate") java.time.LocalDateTime startDate,
                                     @Param("endDate") java.time.LocalDateTime endDate);


    // Find active trips (not ended yet)
    @Query("SELECT t FROM Trip t WHERE t.plannedEndDate IS NULL OR t.plannedEndDate > CURRENT_TIMESTAMP")
    List<Trip> findActiveTrips();

    // Find trips by driver and date range
    @Query("SELECT t FROM Trip t WHERE t.driver.id = :driverId AND t.plannedStartDate BETWEEN :startDate AND :endDate")
    List<Trip> findDriverTripsBetweenDates(@Param("driverId") Long driverId,
                                           @Param("startDate") java.time.LocalDateTime startDate,
                                           @Param("endDate") java.time.LocalDateTime endDate);

    // Add these to TripRepository interface
    List<Trip> findByDriver_IdAndVehicle_Id(Long driverId, Long vehicleId);

    List<Trip> findByDriver_IdAndVehicle_IdAndStatus(Long driverId, Long vehicleId, String status);



}