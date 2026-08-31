// src/main/java/com/pgsa/trailers/repository/FuelEntryRepository.java

package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.FuelEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FuelEntryRepository extends JpaRepository<FuelEntry, Long> {
    
    List<FuelEntry> findByVehicleId(Long vehicleId);
    
    List<FuelEntry> findByVehicleIdAndDateBetween(Long vehicleId, LocalDate startDate, LocalDate endDate);
    
    List<FuelEntry> findByVehicleIdAndDateGreaterThanEqual(Long vehicleId, LocalDate date);
    
    List<FuelEntry> findByVehicleIdAndDateLessThanEqual(Long vehicleId, LocalDate date);
    
    List<FuelEntry> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<FuelEntry> findByDateGreaterThanEqual(LocalDate date);
    
    List<FuelEntry> findByDateLessThanEqual(LocalDate date);
    
    List<FuelEntry> findAllByOrderByDateDesc();
}
