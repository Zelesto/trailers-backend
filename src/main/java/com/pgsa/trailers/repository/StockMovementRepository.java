// src/main/java/com/pgsa/trailers/repository/inventory/StockMovementRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.inventory.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    // Find by item ID - paginated
    Page<StockMovement> findByItemId(Long itemId, Pageable pageable);

    // Find by item ID - all
    List<StockMovement> findByItemId(Long itemId);

    // Find by trip ID
    List<StockMovement> findByTripId(Long tripId);

    // Find by fuel slip ID
    List<StockMovement> findByFuelSlipId(Long fuelSlipId);

    // Find by movement type
    List<StockMovement> findByMovementType(String movementType);

    // Find by date range
    @Query("SELECT sm FROM StockMovement sm WHERE sm.createdAt BETWEEN :startDate AND :endDate")
    List<StockMovement> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    // Find by movement type and date range
    @Query("SELECT sm FROM StockMovement sm WHERE sm.movementType = :type AND sm.createdAt BETWEEN :startDate AND :endDate")
    List<StockMovement> findByMovementTypeAndDateRange(@Param("type") String type,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    // Count by item ID
    long countByItemId(Long itemId);

    // Get total quantity moved by item
    @Query("SELECT COALESCE(SUM(sm.quantity), 0) FROM StockMovement sm WHERE sm.itemId = :itemId AND sm.movementType = :type")
    Integer sumQuantityByItemIdAndMovementType(@Param("itemId") Long itemId,
                                               @Param("type") String type);
}
