// src/main/java/com/pgsa/trailers/repository/VehicleIssueItemRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.inventory.VehicleIssueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleIssueItemRepository extends JpaRepository<VehicleIssueItem, Long> {
    
    List<VehicleIssueItem> findByIssueId(Long issueId);
    
    Optional<VehicleIssueItem> findByIssueIdAndItemId(Long issueId, Long itemId);
    
    @Query("SELECT vi.itemId FROM VehicleIssueItem vi WHERE vi.issue.id = :issueId")
    List<Long> findItemIdsByIssueId(@Param("issueId") Long issueId);
    
    @Query("SELECT vi FROM VehicleIssueItem vi WHERE vi.issue.id = :issueId AND vi.isSwap = true")
    List<VehicleIssueItem> findSwapItemsByIssueId(@Param("issueId") Long issueId);
}
