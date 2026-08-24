// src/main/java/com/pgsa/trailers/repository/inventory/InventoryItemRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.inventory.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findBySku(String sku);
    boolean existsBySku(String sku);
    Long countBySkuStartingWith(String prefix);

    // Find by category
    List<InventoryItem> findByCategory(String category);

    // Find by location
    List<InventoryItem> findByLocationId(Long locationId);

    // Find by consumable
    List<InventoryItem> findByIsConsumableTrue();

    // Find by is active
    List<InventoryItem> findByIsActiveTrue();

    // Find by driver issuable, not held, and active
    List<InventoryItem> findByIsDriverIssuableTrueAndIsHeldFalseAndIsActiveTrue();
    
    // Find by vehicle issuable, not held, and active
    List<InventoryItem> findByIsVehicleIssuableTrueAndIsHeldFalseAndIsActiveTrue();

    // Search items - ONLY ONE definition
    @Query("SELECT i FROM InventoryItem i WHERE " +
           "LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(i.category) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(i.unitOfMeasure) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<InventoryItem> searchItems(@Param("search") String search, Pageable pageable);

    // Count total items
    @Query("SELECT COUNT(i) FROM InventoryItem i")
    Long countTotalItems();

    // Count active items
    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.isActive = true")
    Long countActiveItems();
    
    // Count low stock items
    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.isActive = true AND i.isHeld = false AND i.quantity <= i.minLevel AND i.quantity > 0")
    Long countLowStockItems();
    
    // Count out of stock items
    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.isActive = true AND i.isHeld = false AND (i.quantity IS NULL OR i.quantity <= 0)")
    Long countOutOfStockItems();
    
    // Count held items
    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.isHeld = true")
    Long countHeldItems();

    // Count by category
    @Query("SELECT i.category, COUNT(i) FROM InventoryItem i GROUP BY i.category")
    List<Object[]> countByCategory();

    // Count by location
    @Query("SELECT i.locationId, COUNT(i) FROM InventoryItem i WHERE i.locationId IS NOT NULL GROUP BY i.locationId")
    List<Object[]> countByLocation();

    // Average reorder level - ONLY ONE definition
    @Query("SELECT AVG(i.reorderLevel) FROM InventoryItem i WHERE i.reorderLevel IS NOT NULL")
    Double averageReorderLevel();

    // Calculate total inventory value
    @Query("SELECT SUM(i.quantity * i.unitCost) FROM InventoryItem i WHERE i.unitCost IS NOT NULL AND i.quantity IS NOT NULL")
    BigDecimal calculateTotalValue();

    // Find low stock items
    @Query("SELECT i FROM InventoryItem i WHERE i.isActive = true AND i.isHeld = false AND i.quantity IS NOT NULL AND i.minLevel IS NOT NULL AND i.quantity <= i.minLevel AND i.quantity > 0")
    List<InventoryItem> findLowStockItems();

    // Find out of stock items
    @Query("SELECT i FROM InventoryItem i WHERE i.isActive = true AND i.isHeld = false AND (i.quantity IS NULL OR i.quantity <= 0)")
    List<InventoryItem> findOutOfStockItems();
}
