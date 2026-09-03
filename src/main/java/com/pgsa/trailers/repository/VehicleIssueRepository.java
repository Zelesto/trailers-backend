// src/main/java/com/pgsa/trailers/repository/VehicleIssueRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.inventory.VehicleIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleIssueRepository extends JpaRepository<VehicleIssue, Long> {
    
    List<VehicleIssue> findAllByOrderByIssueDateDesc();
    
    List<VehicleIssue> findByVehicleIdOrderByIssueDateDesc(Long vehicleId);
    
    List<VehicleIssue> findByDriverIdOrderByIssueDateDesc(Long driverId);
    
    List<VehicleIssue> findByStatusNotIn(List<String> statuses);
}
