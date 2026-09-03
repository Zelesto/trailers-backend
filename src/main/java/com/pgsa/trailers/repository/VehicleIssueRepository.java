// src/main/java/com/pgsa/trailers/repository/inventory/VehicleIssueRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.inventory.VehicleIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleIssueRepository extends JpaRepository<VehicleIssue, Long> {
    List<VehicleIssue> findByVehicleIdOrderByIssueDateDesc(Long vehicleId);
    List<VehicleIssue> findByDriverIdOrderByIssueDateDesc(Long driverId);
    List<VehicleIssue> findByTripIdOrderByIssueDateDesc(Long tripId);
    List<VehicleIssue> findAllByOrderByIssueDateDesc();

    // VehicleIssueRepository
    List<VehicleIssue> findByStatusNotIn(List<String> statuses);
    
    // VehicleIssueItemRepository
    Optional<VehicleIssueItem> findByIssueIdAndItemId(Long issueId, Long itemId);
    List<VehicleIssueItem> findByIssueId(Long issueId);
    
    // InventoryItemRepository
    Optional<InventoryItem> findById(Long id);
}
