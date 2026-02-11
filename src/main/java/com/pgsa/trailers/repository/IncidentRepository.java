package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    
    @Query("SELECT i FROM Incident i WHERE i.trip.id = :tripId ORDER BY i.reportedAt DESC")
    List<Incident> findByTripId(@Param("tripId") Long tripId);
    
    @Query("SELECT i FROM Incident i WHERE i.trip.id = :tripId AND i.resolved = false ORDER BY i.reportedAt DESC")
    List<Incident> findActiveIncidentsByTripId(@Param("tripId") Long tripId);
    
    @Query("SELECT i FROM Incident i WHERE i.resolved = :resolved ORDER BY i.reportedAt DESC")
    List<Incident> findByResolved(@Param("resolved") Boolean resolved);
    
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.trip.id = :tripId")
    Long countByTripId(@Param("tripId") Long tripId);
    
    @Query("SELECT i FROM Incident i WHERE i.severity = :severity ORDER BY i.reportedAt DESC")
    List<Incident> findBySeverity(@Param("severity") String severity);
    
    @Query("SELECT i FROM Incident i WHERE i.requiresAssistance = true AND i.resolved = false ORDER BY i.reportedAt DESC")
    List<Incident> findUrgentIncidents();
}
