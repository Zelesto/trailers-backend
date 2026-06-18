// src/main/java/com/pgsa/trailers/repository/PodRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.Pod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PodRepository extends JpaRepository<Pod, Long> {

    Optional<Pod> findByPodNumber(String podNumber);

    List<Pod> findByTripId(Long tripId);

    Page<Pod> findByTripId(Long tripId, Pageable pageable);

    Page<Pod> findByStatus(String status, Pageable pageable);

    Page<Pod> findByCustomerNameContainingIgnoreCase(String customerName, Pageable pageable);

    @Query("SELECT p FROM Pod p WHERE " +
           "LOWER(p.podNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.customerName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Pod> searchPods(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Pod p WHERE p.deliveryDate BETWEEN :startDate AND :endDate")
    List<Pod> findPodsByDateRange(@Param("startDate") LocalDate startDate, 
                                   @Param("endDate") LocalDate endDate);

        long countByStatus(String status);
    
    long countByTripId(Long tripId);  // <-- This is the missing method
    
    boolean existsByTripId(Long tripId);
    
    void deleteByTripId(Long tripId);
}
