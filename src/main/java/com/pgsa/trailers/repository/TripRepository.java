package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.enums.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    // ======================== FIND BY STATUS ========================
    
    List<Trip> findByStatus(TripStatus status);
    
    Page<Trip> findByStatus(TripStatus status, Pageable pageable);
    
    // ======================== FIND BY RELATIONSHIPS ========================
    
    List<Trip> findByDriverId(Long driverId);
    
    List<Trip> findByVehicleId(Long vehicleId);
    
    List<Trip> findByLoadId(Long loadId);
    
    List<Trip> findByDriverIdAndVehicleId(Long driverId, Long vehicleId);
    
    List<Trip> findByDriverIdAndStatus(Long driverId, TripStatus status);
    
    List<Trip> findByVehicleIdAndStatus(Long vehicleId, TripStatus status);
    
    List<Trip> findByDriverIdAndVehicleIdAndStatus(Long driverId, Long vehicleId, TripStatus status);
    
    // ======================== FIND BY TRIP NUMBER ========================
    
    Optional<Trip> findByTripNumber(String tripNumber);
    
    boolean existsByTripNumber(String tripNumber);
    
    // ======================== ADVANCED QUERIES ========================
    
    @Query("SELECT t FROM Trip t WHERE " +
            "(:driverId IS NULL OR t.driver.id = :driverId) AND " +
            "(:vehicleId IS NULL OR t.vehicle.id = :vehicleId) AND " +
            "(:status IS NULL OR t.status = :status)")
    Page<Trip> findByFilters(@Param("driverId") Long driverId,
                             @Param("vehicleId") Long vehicleId,
                             @Param("status") TripStatus status,
                             Pageable pageable);
    
    @Query("SELECT t FROM Trip t WHERE t.plannedStartDate BETWEEN :startDate AND :endDate")
    List<Trip> findByPlannedStartDateBetween(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Trip t WHERE t.actualStartDate BETWEEN :startDate AND :endDate")
    List<Trip> findByActualStartDateBetween(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Trip t WHERE t.driver.id = :driverId AND t.plannedStartDate BETWEEN :startDate AND :endDate")
    List<Trip> findDriverTripsBetweenDates(@Param("driverId") Long driverId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Trip t WHERE t.status IN :statuses")
    List<Trip> findByStatusIn(@Param("statuses") List<TripStatus> statuses);
    
    @Query("SELECT t FROM Trip t WHERE t.status = :status AND t.plannedStartDate <= :date")
    List<Trip> findPlannedTripsUpToDate(@Param("status") TripStatus status,
                                         @Param("date") LocalDateTime date);
    
    // ======================== ACTIVE TRIPS ========================
    
    @Query("SELECT t FROM Trip t WHERE t.status IN ('PLANNED', 'ASSIGNED', 'IN_PROGRESS', 'ACTIVE')")
    List<Trip> findActiveTrips();
    
    @Query("SELECT t FROM Trip t WHERE t.status = 'IN_PROGRESS' OR t.status = 'ACTIVE'")
    List<Trip> findCurrentlyRunningTrips();
    
    // ======================== UPDATE QUERIES ========================
    
    @Modifying
    @Query("UPDATE Trip t SET t.status = :newStatus, t.lastStatusUpdate = :now WHERE t.id = :tripId")
    int updateStatus(@Param("tripId") Long tripId,
                     @Param("newStatus") TripStatus newStatus,
                     @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE Trip t SET t.status = :newStatus, t.lastStatusUpdate = :now WHERE t.id = :tripId AND t.status = :currentStatus")
    int updateStatusIfCurrent(@Param("tripId") Long tripId,
                              @Param("newStatus") TripStatus newStatus,
                              @Param("currentStatus") TripStatus currentStatus,
                              @Param("now") LocalDateTime now);
    
    // ======================== COUNT QUERIES ========================
    
    long countByStatus(TripStatus status);
    
    long countByDriverIdAndStatus(Long driverId, TripStatus status);
    
    long countByVehicleIdAndStatus(Long vehicleId, TripStatus status);
    
    @Query("SELECT COUNT(t) FROM Trip t WHERE t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
    long countByStatusAndDateRange(@Param("status") TripStatus status,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
    
    // ======================== AGGREGATION QUERIES ========================
    
    @Query("SELECT AVG(t.actualDistanceKm) FROM Trip t WHERE t.status = 'COMPLETED' AND t.vehicle.id = :vehicleId")
    Optional<Double> getAverageDistanceForVehicle(@Param("vehicleId") Long vehicleId);
    
    @Query("SELECT SUM(t.actualDistanceKm) FROM Trip t WHERE t.status = 'COMPLETED' AND t.driver.id = :driverId")
    Optional<BigDecimal> getTotalDistanceForDriver(@Param("driverId") Long driverId);
    
    // ======================== EXISTS QUERIES ========================
    
    boolean existsByDriverIdAndStatus(Long driverId, TripStatus status);
    
    boolean existsByVehicleIdAndStatus(Long vehicleId, TripStatus status);
}
