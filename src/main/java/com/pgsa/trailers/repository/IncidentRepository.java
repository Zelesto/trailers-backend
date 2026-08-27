package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    
    // Find all incidents for a trip ordered by reported date (newest first)
    @Query("SELECT i FROM Incident i WHERE i.trip.id = :tripId ORDER BY i.reportedAt DESC")
    List<Incident> findByTripId(@Param("tripId") Long tripId);
    
    // Find incidents for a trip with pagination
    @Query("SELECT i FROM Incident i WHERE i.trip.id = :tripId ORDER BY i.reportedAt DESC")
    Page<Incident> findByTripId(@Param("tripId") Long tripId, Pageable pageable);
    
    // Find active (unresolved) incidents for a trip
    @Query("SELECT i FROM Incident i WHERE i.trip.id = :tripId AND i.resolved = false ORDER BY i.reportedAt DESC")
    List<Incident> findActiveIncidentsByTripId(@Param("tripId") Long tripId);
    
    // Find incidents by resolved status
    @Query("SELECT i FROM Incident i WHERE i.resolved = :resolved ORDER BY i.reportedAt DESC")
    List<Incident> findByResolved(@Param("resolved") Boolean resolved);
    
    // Count incidents by trip
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.trip.id = :tripId")
    Long countByTripId(@Param("tripId") Long tripId);
    
    // Count incidents by trip and resolved status
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.trip.id = :tripId AND i.resolved = :resolved")
    Long countByTripIdAndResolved(@Param("tripId") Long tripId, @Param("resolved") Boolean resolved);
    
    // Find incidents by severity
    @Query("SELECT i FROM Incident i WHERE i.severity = :severity ORDER BY i.reportedAt DESC")
    List<Incident> findBySeverity(@Param("severity") String severity);
    
    // Find urgent incidents (requires assistance and not resolved)
    @Query("SELECT i FROM Incident i WHERE i.requiresAssistance = true AND i.resolved = false ORDER BY i.reportedAt DESC")
    List<Incident> findUrgentIncidents();
}
