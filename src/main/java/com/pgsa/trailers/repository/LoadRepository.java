package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.enums.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoadRepository extends JpaRepository<Load, Long> {

    // ======================== FIND BY LOAD NUMBER ========================
    
    Optional<Load> findByLoadNumber(String loadNumber);
    
    boolean existsByLoadNumber(String loadNumber);
    
    // ======================== FIND BY STATUS ========================
    
    List<Load> findByStatus(String status);
    
    Page<Load> findByStatus(String status, Pageable pageable);
    
    // ======================== FIND BY DATE RANGE ========================
    
    List<Load> findByLoadingDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<Load> findByUnloadingDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // ======================== FIND BY TRIP ========================
    
    @Query("SELECT DISTINCT l FROM Load l JOIN l.trips t WHERE t.id = :tripId")
    Optional<Load> findByTripId(@Param("tripId") Long tripId);
    
    @Query("SELECT l FROM Load l JOIN l.trips t WHERE t.status = :status")
    List<Load> findByTripStatus(@Param("status") TripStatus status);
    
    // ======================== ACTIVE LOADS ========================
    
    @Query("SELECT l FROM Load l WHERE l.status IN ('PENDING', 'IN_TRANSIT', 'LOADING')")
    List<Load> findActiveLoads();
    
    @Query("SELECT l FROM Load l WHERE l.loadingDate <= :now AND (l.unloadingDate IS NULL OR l.unloadingDate > :now)")
    List<Load> findCurrentLoads(@Param("now") LocalDateTime now);
    
    // ======================== COUNT QUERIES ========================
    
    long countByStatus(String status);
    
    @Query("SELECT COUNT(l) FROM Load l WHERE l.status = :status AND l.createdAt BETWEEN :startDate AND :endDate")
    long countByStatusAndDateRange(@Param("status") String status,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);
    
    // ======================== AGGREGATION QUERIES ========================
    
    @Query("SELECT SUM(l.weightKg) FROM Load l WHERE l.status = 'COMPLETED'")
    Optional<Double> getTotalWeightOfCompletedLoads();
    
    @Query("SELECT AVG(l.weightKg) FROM Load l WHERE l.status = 'COMPLETED'")
    Optional<Double> getAverageWeightOfCompletedLoads();
    
    // ======================== EXISTENCE CHECKS ========================
    
    boolean existsByLoadNumberAndStatus(String loadNumber, String status);
    
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Load l JOIN l.trips t WHERE t.id = :tripId")
    boolean hasLoadAssociatedWithTrip(@Param("tripId") Long tripId);
    
    // ======================== BULK OPERATIONS ========================
    
    @Query("UPDATE Load l SET l.status = :newStatus WHERE l.id IN :loadIds AND l.status = :currentStatus")
    int updateStatusBulk(@Param("loadIds") List<Long> loadIds,
                         @Param("newStatus") String newStatus,
                         @Param("currentStatus") String currentStatus);
    
    // ======================== SEARCH ========================
    
    @Query("SELECT l FROM Load l WHERE " +
           "(:loadNumber IS NULL OR l.loadNumber LIKE CONCAT('%', :loadNumber, '%')) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:commodityType IS NULL OR l.commodityType = :commodityType)")
    Page<Load> searchLoads(@Param("loadNumber") String loadNumber,
                           @Param("status") String status,
                           @Param("commodityType") String commodityType,
                           Pageable pageable);
}
