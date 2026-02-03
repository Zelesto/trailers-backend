package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.Pod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PodRepository extends JpaRepository<Pod, Long> {

    long countByTripId(Long tripId);
}
