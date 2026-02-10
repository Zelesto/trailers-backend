package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.TripMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TripMetricsRepository extends JpaRepository<TripMetrics, Long> {
    Optional<TripMetrics> findByTripId(Long tripId);
    void deleteByTripId(Long tripId);
    boolean existsByTripId(Long tripId);
}
