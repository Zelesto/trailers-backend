package com.pgsa.trailers.repository;

import com.pgsa.trailers.dto.TripKpiDTO;
import com.pgsa.trailers.entity.ops.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TripKpiRepository extends JpaRepository<Trip, Long> {

    @Query("""
        select new com.pgsa.trailers.dto.TripKpiDTO(
            t.id,
            t.tripNumber,
            t.status,
            t.plannedStartDate,
            tm.totalDistanceKm,
            tm.fuelUsedLiters,
            tm.revenueAmount,
            tm.costAmount,
            (tm.revenueAmount - tm.costAmount)
        )
        from Trip t
        join t.metrics tm
        where t.plannedStartDate >= :from
          and t.plannedStartDate <= :to
    """)
    List<TripKpiDTO> findTripKpis(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
