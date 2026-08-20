// src/main/java/com/pgsa/trailers/repository/LoadRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.Load;
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
public interface LoadRepository extends JpaRepository<Load, Long> {

    // ============================================================
    // CONSTANTS FOR STATUS VALUES (from enum_master table)
    // ============================================================
    String STATUS_PENDING = "PENDING";
    String STATUS_PLANNED = "PLANNED";
    String STATUS_IN_TRANSIT = "IN_TRANSIT";
    String STATUS_LOADING = "LOADING";
    String STATUS_DELIVERED = "DELIVERED";
    String STATUS_COMPLETED = "COMPLETED";
    String STATUS_CANCELLED = "CANCELLED";

    // ======================== FIND BY LOAD NUMBER ========================
    
    Optional<Load> findByLoadNumber(String loadNumber);
    
    boolean existsByLoadNumber(String loadNumber);
    
    // ======================== FIND BY REFERENCE NUMBER ========================
    
    Optional<Load> findByReferenceNumber(String referenceNumber);
    
    boolean existsByReferenceNumber(String referenceNumber);
    
    @Query("SELECT l FROM Load l WHERE l.referenceNumber LIKE CONCAT('%', :referenceNumber, '%')")
    List<Load> findByReferenceNumberContaining(@Param("referenceNumber") String referenceNumber);
    
    // ======================== FIND BY STATUS - FIXED ========================
    
    @Query("SELECT l FROM Load l WHERE l.status = :status")
    List<Load> findByStatus(@Param("status") String status);
    
    @Query("SELECT l FROM Load l WHERE l.status = :status")
    Page<Load> findLoadsByStatus(@Param("status") String status, Pageable pageable);
    
    Page<Load> findByStatus(String status, Pageable pageable);
    
    @Query("SELECT l FROM Load l WHERE l.status IN :statuses")
    List<Load> findByStatusIn(@Param("statuses") List<String> statuses);
    
    // ======================== FIND BY DATE RANGE ========================
    
    List<Load> findByLoadingDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<Load> findByUnloadingDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT l FROM Load l WHERE l.loadingDate <= :endDate AND (l.unloadingDate IS NULL OR l.unloadingDate >= :startDate)")
    List<Load> findActiveLoadsBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    // ======================== FIND BY TRIP - FIXED ========================
    
    @Query("SELECT DISTINCT l FROM Load l JOIN l.trips t WHERE t.id = :tripId")
    Optional<Load> findByTripId(@Param("tripId") Long tripId);
    
    @Query("SELECT l FROM Load l JOIN l.trips t WHERE t.status = :status")
    List<Load> findByTripStatus(@Param("status") String status);
    
    @Query("SELECT l FROM Load l JOIN l.trips t WHERE t.tripNumber = :tripNumber")
    Optional<Load> findByTripNumber(@Param("tripNumber") String tripNumber);
    
    // ======================== ACTIVE LOADS - FIXED ========================
    
    @Query("SELECT l FROM Load l WHERE l.status IN ('PENDING', 'IN_TRANSIT', 'LOADING')")
    List<Load> findActiveLoads();
    
    @Query("SELECT l FROM Load l WHERE l.status IN ('PENDING', 'IN_TRANSIT', 'LOADING') ORDER BY l.loadingDate ASC")
    List<Load> findActiveLoadsOrderByDate();
    
    @Query("SELECT l FROM Load l WHERE l.loadingDate <= :now AND (l.unloadingDate IS NULL OR l.unloadingDate > :now)")
    List<Load> findCurrentLoads(@Param("now") LocalDateTime now);
    
    // ======================== CUSTOMER - FIXED ========================
       
    List<Load> findByCustomerId(Long customerId);
    
    @Query("SELECT l FROM Load l WHERE l.customerId = :customerId ORDER BY l.loadingDate DESC")
    List<Load> findByCustomerIdOrderByDateDesc(@Param("customerId") Long customerId);
    
    @Query("SELECT l FROM Load l WHERE l.customerId = :customerId AND l.loadingDate BETWEEN :startDate AND :endDate")
    List<Load> findByCustomerIdAndLoadingDateBetween(
            @Param("customerId") Long customerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT l FROM Load l WHERE l.customerId = :customerId AND l.status = :status")
    List<Load> findByCustomerIdAndStatus(@Param("customerId") Long customerId, 
                                         @Param("status") String status);
    
    // ======================== COUNT QUERIES - FIXED ========================
    
    @Query("SELECT COUNT(l) FROM Load l WHERE l.status = :status")
    long countByStatus(@Param("status") String status);
    
    @Query("SELECT COUNT(l) FROM Load l WHERE l.status = :status")
    long countByStatusString(@Param("status") String status);
    
    @Query("SELECT COUNT(l) FROM Load l WHERE l.status = :status AND l.createdAt BETWEEN :startDate AND :endDate")
    long countByStatusAndDateRange(@Param("status") String status,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(l) FROM Load l WHERE l.customerId = :customerId")
    long countByCustomerId(@Param("customerId") Long customerId);
    
    // ======================== AGGREGATION QUERIES - FIXED ========================
    
    @Query("SELECT SUM(l.weightKg) FROM Load l WHERE l.status = 'COMPLETED'")
    Optional<Double> getTotalWeightOfCompletedLoads();
    
    @Query("SELECT AVG(l.weightKg) FROM Load l WHERE l.status = 'COMPLETED'")
    Optional<Double> getAverageWeightOfCompletedLoads();
    
    @Query("SELECT SUM(l.totalDepotKm) FROM Load l")
    Optional<BigDecimal> getTotalDepotKmAllLoads();
    
    @Query("SELECT SUM(l.totalFromDepotKm) FROM Load l WHERE l.status = :status")
    Optional<BigDecimal> getTotalFromDepotKmByStatus(@Param("status") String status);
    
    // ======================== EXISTENCE CHECKS - FIXED ========================
    
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Load l WHERE l.loadNumber = :loadNumber AND l.status = :status")
    boolean existsByLoadNumberAndStatus(@Param("loadNumber") String loadNumber, 
                                        @Param("status") String status);
    
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Load l JOIN l.trips t WHERE t.id = :tripId")
    boolean hasLoadAssociatedWithTrip(@Param("tripId") Long tripId);
    
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Load l WHERE l.referenceNumber = :referenceNumber AND l.id != :loadId")
    boolean existsByReferenceNumberAndIdNot(@Param("referenceNumber") String referenceNumber, 
                                             @Param("loadId") Long loadId);
    
    // ======================== BULK OPERATIONS - FIXED ========================
    
    @Modifying
    @Query("UPDATE Load l SET l.status = :newStatus WHERE l.id IN :loadIds AND l.status = :currentStatus")
    int updateStatusBulk(@Param("loadIds") List<Long> loadIds,
                         @Param("newStatus") String newStatus,
                         @Param("currentStatus") String currentStatus);
    
    @Modifying
    @Query("UPDATE Load l SET l.status = :status, l.lastStatusUpdate = CURRENT_TIMESTAMP WHERE l.id = :loadId")
    int updateStatus(@Param("loadId") Long loadId, @Param("status") String status);
    
    @Modifying
    @Query("UPDATE Load l SET l.tripsCount = :count WHERE l.id = :loadId")
    int updateTripsCount(@Param("loadId") Long loadId, @Param("count") Integer count);
    
    // ======================== SEARCH - FIXED ========================
    
    @Query("SELECT l FROM Load l WHERE " +
           "LOWER(l.loadNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.referenceNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.commodityType) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Load> searchLoads(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT l FROM Load l WHERE " +
           "LOWER(l.loadNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.referenceNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.commodityType) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "ORDER BY l.createdAt DESC")
    List<Load> searchLoadsOrderByDateDesc(@Param("search") String search);
    
    @Query("SELECT l FROM Load l WHERE " +
           "(:loadNumber IS NULL OR l.loadNumber LIKE CONCAT('%', :loadNumber, '%')) AND " +
           "(:referenceNumber IS NULL OR l.referenceNumber LIKE CONCAT('%', :referenceNumber, '%')) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:commodityType IS NULL OR l.commodityType = :commodityType) AND " +
           "(:customerId IS NULL OR l.customerId = :customerId)")
    Page<Load> searchLoadsAdvanced(@Param("loadNumber") String loadNumber,
                                   @Param("referenceNumber") String referenceNumber,
                                   @Param("status") String status,
                                   @Param("commodityType") String commodityType,
                                   @Param("customerId") Long customerId,
                                   Pageable pageable);
    
    // ======================== TOP LOADS ========================
    
    @Query("SELECT l FROM Load l ORDER BY l.tripsCount DESC")
    List<Load> findTopLoadsByTripCount(Pageable pageable);
    
    @Query("SELECT l FROM Load l WHERE l.status = 'COMPLETED' ORDER BY l.totalDepotKm DESC")
    List<Load> findTopLoadsByDistance(Pageable pageable);
    
    // ======================== LOAD WITH TRIPS COUNT ========================
    
    @Query("SELECT l, COUNT(t) as tripCount FROM Load l LEFT JOIN l.trips t GROUP BY l.id")
    List<Object[]> findLoadsWithTripCount();
    
    @Query("SELECT l FROM Load l WHERE SIZE(l.trips) = 0")
    List<Load> findEmptyLoads();
    
    @Query("SELECT l FROM Load l WHERE SIZE(l.trips) > 0 AND SIZE(l.trips) < :maxTrips")
    List<Load> findLoadsWithTripCountBetween(@Param("maxTrips") int maxTrips);
}
