package com.pgsa.trailers.repository.assets;

import com.pgsa.trailers.entity.assets.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {
    List<VehicleType> findByIsActiveTrueOrderByNameAsc();
}
