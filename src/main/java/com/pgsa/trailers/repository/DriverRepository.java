package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.assets.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {



    // Add this method
    Optional<Driver> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);
    //Optional<Driver> findByLicenseNumber(String licenseNumber);
    //List<Driver> findByFullNameContainingIgnoreCase(String name);
    //List<Driver> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);
    // Optional: find active drivers
    // List<Driver> findByStatus(String status);
}
