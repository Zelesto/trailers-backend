// src/main/java/com/pgsa/trailers/repository/inventory/InventoryLocationRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.inventory.InventoryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryLocationRepository extends JpaRepository<InventoryLocation, Long> {
}
