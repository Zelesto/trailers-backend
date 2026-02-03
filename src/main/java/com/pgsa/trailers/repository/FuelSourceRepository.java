package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.FuelSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuelSourceRepository extends JpaRepository<FuelSource, Long> {
    List<FuelSource> findByNameContainingIgnoreCase(String name);
    Optional<FuelSource> findByNameIgnoreCase(String name);
}