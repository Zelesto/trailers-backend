package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.inventory.StockCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockCountRepository extends JpaRepository<StockCount, Long> {

    boolean existsByIdAndStatus(Long id, String status);

}
