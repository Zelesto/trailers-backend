package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.assets.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {


    Optional<Vehicle> findByRegistrationNumberIgnoreCase(String registrationNumber);
    // Find vehicle by registration
    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);


    // Find vehicles by status - UNCOMMENT THIS
    List<Vehicle> findByStatus(String status);

    // Find active vehicles
    List<Vehicle> findByStatusIn(List<String> statuses);
}