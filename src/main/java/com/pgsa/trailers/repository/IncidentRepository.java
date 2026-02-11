package com.pgsa.trailers.repository.ops;

import com.pgsa.trailers.entity.ops.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByTripId(Long tripId);
    
    @Query("SELECT i FROM Incident i WHERE i.trip.id = :tripId AND i.resolved = false")
    List<Incident> findActiveIncidentsByTripId(@Param("tripId") Long tripId);
    
    List<Incident> findByResolved(Boolean resolved);
    
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.trip.id = :tripId")
    long countByTripId(@Param("tripId") Long tripId);
}
