// src/main/java/com/pgsa/trailers/repository/inventory/InventoryItemRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByCategory(String category);

    List<InventoryItem> findByLocationId(Long locationId);

    List<InventoryItem> findByIsConsumableTrue();

    @Query("SELECT i FROM InventoryItem i WHERE " +
           "LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(i.category) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<InventoryItem> searchItems(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(i) FROM InventoryItem i")
    Long countTotalItems();

    @Query("SELECT i.category, COUNT(i) FROM InventoryItem i GROUP BY i.category")
    List<Object[]> countByCategory();

    @Query("SELECT i.locationId, COUNT(i) FROM InventoryItem i WHERE i.locationId IS NOT NULL GROUP BY i.locationId")
    List<Object[]> countByLocation();

    @Query("SELECT AVG(i.reorderLevel) FROM InventoryItem i WHERE i.reorderLevel IS NOT NULL")
    Double averageReorderLevel();
}
