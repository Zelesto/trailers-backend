package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.inventory.StockMovement;
import com.pgsa.trailers.enums.StockMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query("SELECT sm FROM StockMovement sm WHERE sm.item.id = :itemId")
    List<StockMovement> findByItemId(@Param("itemId") Long itemId);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.item.id = :itemId AND sm.location.id = :locationId")
    List<StockMovement> findByItemIdAndLocationId(@Param("itemId") Long itemId,
                                                  @Param("locationId") Long locationId);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.movementType = :movementType")
    List<StockMovement> findByMovementType(@Param("movementType") StockMovementType movementType);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.referenceType = :referenceType AND sm.referenceId = :referenceId")
    List<StockMovement> findByReferenceTypeAndReferenceId(@Param("referenceType") String referenceType,
                                                          @Param("referenceId") Long referenceId);
}
