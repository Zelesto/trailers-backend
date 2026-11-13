package com.pgsa.trailers.repository.ops;

import com.pgsa.trailers.entity.ops.Commodity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommodityRepository extends JpaRepository<Commodity, Long> {
    List<Commodity> findByIsActiveTrueOrderByNameAsc();
}
