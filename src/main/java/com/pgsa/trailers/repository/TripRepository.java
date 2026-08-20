package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.Trip;
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
import java.math.BigDecimal;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    // ============================================================
    // COUNT QUERIES
    // ============================================================
    
    @Query("SELECT t.status, COUNT(t) FROM Trip t GROUP BY t.status")
    List<Object[]> countByStatusGrouped();
    
    long countByStatus(TripStatus status);
    
    long countByDriverIdAndStatus(Long driverId, TripStatus status);
    
    long countByVehicleIdAndStatus(Long vehicleId, TripStatus status);
    
    @Query("SELECT COUNT(t) FROM Trip t WHERE t.status = :status AND t.createdAt BETWEEN :startDate AND :endDate")
    long countByStatusAndDateRange(@Param("status") TripStatus status,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
    
    // ============================================================
    // JOIN FETCH QUERIES - For single entity only (not paginated)
    // ============================================================
    
    @Query("SELECT t FROM Trip t")
    Page<Trip> findAllWithCustomer(Pageable pageable);
    
    @Query("SELECT t FROM Trip t WHERE t.status IN :statuses")
    Page<Trip> findByStatusIn(@Param("statuses") List<TripStatus> statuses, Pageable pageable);
    
    @Query("SELECT t FROM Trip t WHERE t.customerId = :customerId")
    Page<Trip> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);
    
    @Query("SELECT t FROM Trip t " +
           "LEFT JOIN FETCH t.customer c " +
           "WHERE t.id = :id")
    Optional<Trip> findByIdWithCustomer(@Param("id") Long id);
    
    @Query("SELECT t FROM Trip t " +
           "LEFT JOIN FETCH t.customer c " +
           "LEFT JOIN FETCH t.vehicle v " +
           "LEFT JOIN FETCH t.driver d " +
           "LEFT JOIN FETCH t.supervisor s " +
           "LEFT JOIN FETCH t.load l " +
           "LEFT JOIN FETCH t.metrics m " +
           "WHERE t.id = :id")
    Optional<Trip> findByIdWithAllRelations(@Param("id") Long id);
    
    // ============================================================
    // SEARCH WITH JOIN FETCH
    // ============================================================
    
    @Query("SELECT t FROM Trip t WHERE " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR " +
           "LOWER(t.tripNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.originCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.destinationCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.customer.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.referenceNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Trip> searchTripsWithCustomer(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // ============================================================
    // FIND BY STATUS
    // ============================================================
    
    List<Trip> findByStatus(String status);
    
    List<Trip> findByStatusOrderByIdDesc(String status);
    
    Page<Trip> findByStatus(String status, Pageable pageable);

    @Query("SELECT t FROM Trip t WHERE t.status IN :statuses")
    List<Trip> findTripsByStatusIn(@Param("statuses") List<String> statuses);
    
    @Query("SELECT t FROM Trip t WHERE t.status IN :statuses ORDER BY t.id DESC")
    List<Trip> findTripsByStatusInOrderByIdDesc(@Param("statuses") List<String> statuses);
    
    // ============================================================
    // FIND ALL WITH SORTING
    // ============================================================
    
    @Query("SELECT t FROM Trip t ORDER BY t.id DESC")
    List<Trip> findAllOrderByIdDesc();
    
    @Query("SELECT t FROM Trip t ORDER BY t.id DESC")
    Page<Trip> findAllOrderByIdDesc(Pageable pageable);

    @Query("SELECT t FROM Trip t ORDER BY t.id DESC")
    Page<Trip> findAllTrips(Pageable pageable);
    
    // ============================================================
    // FIND BY RELATIONSHIPS
    // ============================================================
    
    List<Trip> findByDriverId(Long driverId);
    
    List<Trip> findByDriverIdOrderByIdDesc(Long driverId);
    
    List<Trip> findByVehicleId(Long vehicleId);
    
    List<Trip> findByVehicleIdOrderByIdDesc(Long vehicleId);
    
    List<Trip> findByLoadId(String loadId);
    
    List<Trip> findByDriverIdAndVehicleId(Long driverId, Long vehicleId);
    
    List<Trip> findByDriverIdAndStatus(Long driverId, TripStatus status);
    
    List<Trip> findByVehicleIdAndStatus(Long vehicleId, TripStatus status);
    
    List<Trip> findByDriverIdAndVehicleIdAndStatus(Long driverId, Long vehicleId, TripStatus status);
    
    // ============================================================
    // FIND BY TRIP NUMBER
    // ============================================================
    
    Optional<Trip> findByTripNumber(String tripNumber);

    @Query("SELECT t.tripNumber FROM Trip t WHERE t.id = :tripId")
    Optional<String> findTripNumberById(@Param("tripId") Long tripId);
    
    boolean existsByTripNumber(String tripNumber);
    
    // ============================================================
    // LOAD QUERIES
    // ============================================================
    
    Page<Trip> findByLoadIdIsNull(Pageable pageable);

    @Query("SELECT t FROM Trip t WHERE t.loadId IS NULL OR t.loadId = ''")
    Page<Trip> findByLoadIdIsNullOrEmpty(Pageable pageable);
    
    @Query("SELECT t FROM Trip t WHERE t.loadId IS NULL OR t.loadId = '' ORDER BY t.id DESC")
    List<Trip> findTripsWithoutLoadOrderByIdDesc();
    
    @Query("SELECT t FROM Trip t WHERE (t.loadId IS NULL OR t.loadId = '') AND t.status IN :statuses")
    Page<Trip> findByLoadIdIsNullAndStatusIn(@Param("statuses") List<TripStatus> statuses, Pageable pageable);
    
    @Query("SELECT t FROM Trip t WHERE t.customerId = :customerId " +
           "AND t.plannedStartDate BETWEEN :startDate AND :endDate " +
           "AND (t.loadId IS NULL OR t.loadId = '')")
    List<Trip> findByCustomerIdAndPlannedStartDateBetweenAndLoadIsNull(
        @Param("customerId") Long customerId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    List<Trip> findByLoadNumber(String loadNumber);

    Long countByCustomerId(Long customerId);

    @Query("SELECT t FROM Trip t WHERE t.loadId IS NULL OR t.loadId = ''")
    List<Trip> findTripsWithoutLoad();

    @Query("SELECT t FROM Trip t WHERE t.loadId IS NULL OR t.loadId = ''")
    Page<Trip> findTripsWithoutLoad(Pageable pageable);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.loadId = :loadId")
    long countByLoadId(@Param("loadId") String loadId);
    
    // ============================================================
    // SEARCH QUERIES
    // ============================================================

    @Query("SELECT t FROM Trip t WHERE " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR " +
           "LOWER(t.tripNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.originCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.destinationCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.customer.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.referenceNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Trip> searchTrips(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT t FROM Trip t WHERE " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR " +
           "LOWER(t.tripNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.originCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.destinationCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.customer.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.referenceNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Trip> searchTripsSafe(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT t FROM Trip t WHERE " +
           "LOWER(t.tripNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.originCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.destinationCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.customer.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.referenceNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY t.id DESC")
    List<Trip> searchTripsOrderByIdDesc(@Param("searchTerm") String searchTerm);
    
    // ============================================================
    // FILTER QUERIES - With pagination
    // ============================================================
    
    @Query("SELECT t FROM Trip t WHERE " +
           "LOWER(t.originCity) = LOWER(:city) OR " +
           "LOWER(t.destinationCity) = LOWER(:city)")
    Page<Trip> findByOriginCityOrDestinationCity(@Param("city") String city, Pageable pageable);
    
    @Query("SELECT t FROM Trip t WHERE LOWER(t.customer.name) LIKE LOWER(CONCAT('%', :customer, '%'))")
    Page<Trip> findByCustomerNameContaining(@Param("customer") String customer, Pageable pageable);
    
    @Query("SELECT t FROM Trip t WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(t.tripNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.originCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.destinationCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.customer.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.referenceNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:city IS NULL OR LOWER(t.originCity) = LOWER(:city) OR LOWER(t.destinationCity) = LOWER(:city)) " +
           "AND (:customer IS NULL OR LOWER(t.customer.name) = LOWER(:customer)) " +
           "ORDER BY t.id DESC")
    Page<Trip> findWithFilters(@Param("searchTerm") String searchTerm,
                               @Param("status") TripStatus status,
                               @Param("city") String city,
                               @Param("customer") String customer,
                               Pageable pageable);
    
    @Query("SELECT t FROM Trip t WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(t.tripNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.originCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.destinationCity) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.customer.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.referenceNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:city IS NULL OR LOWER(t.originCity) = LOWER(:city) OR LOWER(t.destinationCity) = LOWER(:city)) " +
           "AND (:customer IS NULL OR LOWER(t.customer.name) = LOWER(:customer)) " +
           "ORDER BY t.id DESC")
    Page<Trip> findWithFiltersOrderByIdDesc(@Param("searchTerm") String searchTerm,
                                            @Param("status") TripStatus status,
                                            @Param("city") String city,
                                            @Param("customer") String customer,
                                            Pageable pageable);
    
    // ============================================================
    // ADVANCED QUERIES
    // ============================================================
    
    @Query("SELECT t FROM Trip t WHERE " +
            "(:driverId IS NULL OR t.driver.id = :driverId) AND " +
            "(:vehicleId IS NULL OR t.vehicle.id = :vehicleId) AND " +
            "(:status IS NULL OR t.status = :status)")
    Page<Trip> findByFilters(@Param("driverId") Long driverId,
                             @Param("vehicleId") Long vehicleId,
                             @Param("status") TripStatus status,
                             Pageable pageable);
    
    @Query("SELECT t FROM Trip t WHERE " +
            "(:driverId IS NULL OR t.driver.id = :driverId) AND " +
            "(:vehicleId IS NULL OR t.vehicle.id = :vehicleId) AND " +
            "(:status IS NULL OR t.status = :status) " +
            "ORDER BY t.id DESC")
    List<Trip> findByFiltersOrderByIdDesc(@Param("driverId") Long driverId,
                                          @Param("vehicleId") Long vehicleId,
                                          @Param("status") TripStatus status);
    
    @Query("SELECT t FROM Trip t WHERE t.plannedStartDate BETWEEN :startDate AND :endDate")
    List<Trip> findByPlannedStartDateBetween(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Trip t WHERE t.plannedStartDate BETWEEN :startDate AND :endDate ORDER BY t.id DESC")
    List<Trip> findByPlannedStartDateBetweenOrderByIdDesc(@Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Trip t WHERE t.actualStartDate BETWEEN :startDate AND :endDate")
    List<Trip> findByActualStartDateBetween(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Trip t WHERE t.actualStartDate BETWEEN :startDate AND :endDate ORDER BY t.id DESC")
    List<Trip> findByActualStartDateBetweenOrderByIdDesc(@Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Trip t WHERE t.driver.id = :driverId AND t.plannedStartDate BETWEEN :startDate AND :endDate")
    List<Trip> findDriverTripsBetweenDates(@Param("driverId") Long driverId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Trip t WHERE t.driver.id = :driverId AND t.plannedStartDate BETWEEN :startDate AND :endDate ORDER BY t.id DESC")
    List<Trip> findDriverTripsBetweenDatesOrderByIdDesc(@Param("driverId") Long driverId,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);

    // ============================================================
    // DRIVER QUERIES WITH PAGINATION
    // ============================================================
    
    /**
     * Find trips by driver ID with pagination using JPQL
     */
    @Query("SELECT t FROM Trip t WHERE t.driver.id = :driverId")
    Page<Trip> findTripsByDriverId(@Param("driverId") Long driverId, Pageable pageable);
    
    /**
     * Find trips by driver ID and status list with pagination using JPQL
     */
    @Query("SELECT t FROM Trip t WHERE t.driver.id = :driverId AND t.status IN :statuses")
    Page<Trip> findTripsByDriverIdAndStatusIn(
        @Param("driverId") Long driverId,
        @Param("statuses") List<TripStatus> statuses,
        Pageable pageable
    );
    
    /**
     * Find trips by driver ID with pagination using Native Query
     */
    @Query(value = "SELECT * FROM trip WHERE driver_id = :driverId ORDER BY id DESC", 
           countQuery = "SELECT COUNT(*) FROM trip WHERE driver_id = :driverId",
           nativeQuery = true)
    Page<Trip> findTripsByDriverIdNative(@Param("driverId") Long driverId, Pageable pageable);
    
    /**
     * Find trips by driver ID and status list with pagination using Native Query
     */
    @Query(value = "SELECT * FROM trip WHERE driver_id = :driverId AND status IN :statuses ORDER BY id DESC",
           countQuery = "SELECT COUNT(*) FROM trip WHERE driver_id = :driverId AND status IN :statuses",
           nativeQuery = true)
    Page<Trip> findTripsByDriverIdAndStatusInNative(
        @Param("driverId") Long driverId,
        @Param("statuses") List<String> statuses,
        Pageable pageable
    );


        // ============================================================
    // VEHICLE QUERIES WITH PAGINATION
    // ============================================================

    /**
 * Find trips by vehicle ID with pagination
 */
@Query("SELECT t FROM Trip t WHERE t.vehicle.id = :vehicleId")
Page<Trip> findTripsByVehicleId(@Param("vehicleId") Long vehicleId, Pageable pageable);

/**
 * Find trips by vehicle ID and status list with pagination
 */
@Query("SELECT t FROM Trip t WHERE t.vehicle.id = :vehicleId AND t.status IN :statuses")
Page<Trip> findTripsByVehicleIdAndStatusIn(
    @Param("vehicleId") Long vehicleId,
    @Param("statuses") List<TripStatus> statuses,
    Pageable pageable
);

/**
 * Find trips by vehicle ID with pagination using Native Query
 */
@Query(value = "SELECT * FROM trip WHERE vehicle_id = :vehicleId ORDER BY id DESC", 
       countQuery = "SELECT COUNT(*) FROM trip WHERE vehicle_id = :vehicleId",
       nativeQuery = true)
Page<Trip> findTripsByVehicleIdNative(@Param("vehicleId") Long vehicleId, Pageable pageable);

/**
 * Find trips by vehicle ID and status list with pagination using Native Query
 */
@Query(value = "SELECT * FROM trip WHERE vehicle_id = :vehicleId AND status IN :statuses ORDER BY id DESC",
       countQuery = "SELECT COUNT(*) FROM trip WHERE vehicle_id = :vehicleId AND status IN :statuses",
       nativeQuery = true)
Page<Trip> findTripsByVehicleIdAndStatusInNative(
    @Param("vehicleId") Long vehicleId,
    @Param("statuses") List<String> statuses,
    Pageable pageable
);
    
    // ============================================================
    // ACTIVE TRIPS
    // ============================================================
    
    @Query("SELECT t FROM Trip t WHERE t.status IN ('PLANNED', 'ASSIGNED', 'IN_PROGRESS', 'ACTIVE')")
    List<Trip> findActiveTrips();
    
    @Query("SELECT t FROM Trip t WHERE t.status IN ('PLANNED', 'ASSIGNED', 'IN_PROGRESS', 'ACTIVE') ORDER BY t.id DESC")
    List<Trip> findActiveTripsOrderByIdDesc();
    
    @Query("SELECT t FROM Trip t WHERE t.status = 'IN_PROGRESS' OR t.status = 'ACTIVE'")
    List<Trip> findCurrentlyRunningTrips();
    
    @Query("SELECT t FROM Trip t WHERE t.status = 'IN_PROGRESS' OR t.status = 'ACTIVE' ORDER BY t.id DESC")
    List<Trip> findCurrentlyRunningTripsOrderByIdDesc();
    
    // ============================================================
    // UPDATE QUERIES
    // ============================================================
    
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
    
    // ============================================================
    // AGGREGATION QUERIES
    // ============================================================

    @Query("SELECT MAX(t.tripNumber) FROM Trip t WHERE t.tripNumber LIKE CONCAT('TRP-', :year, '-%')")
    String findMaxTripNumberForYear(@Param("year") int year);
    
    @Query("SELECT AVG(t.actualDistanceKm) FROM Trip t WHERE t.status = 'COMPLETED' AND t.vehicle.id = :vehicleId")
    Optional<Double> getAverageDistanceForVehicle(@Param("vehicleId") Long vehicleId);
    
    @Query("SELECT SUM(t.actualDistanceKm) FROM Trip t WHERE t.status = 'COMPLETED' AND t.driver.id = :driverId")
    Optional<BigDecimal> getTotalDistanceForDriver(@Param("driverId") Long driverId);

    // ============================================================
    // EXISTS QUERIES
    // ============================================================
    
    boolean existsByDriverIdAndStatus(Long driverId, TripStatus status);
    
    boolean existsByVehicleIdAndStatus(Long vehicleId, TripStatus status);
}
