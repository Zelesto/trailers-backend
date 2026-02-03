package com.pgsa.trailers.repository;


import com.pgsa.trailers.dto.TripAggregateKpiDTO;
import com.pgsa.trailers.entity.ops.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface TripAggregateKpiRepository extends JpaRepository<Trip, Long> {

    @Query("""
        select new com.pgsa.trailers.dto.TripAggregateKpiDTO(
            count(t.id),
            coalesce(sum(tm.totalDistanceKm), 0),
            coalesce(sum(tm.fuelUsedLiters), 0),
            coalesce(sum(tm.revenueAmount), 0),
            coalesce(sum(tm.costAmount), 0),
            coalesce(sum(tm.revenueAmount - tm.costAmount), 0),
            coalesce(avg(tm.totalDistanceKm), 0),
            coalesce(avg(tm.fuelUsedLiters), 0),
            coalesce(avg(tm.revenueAmount - tm.costAmount), 0)
        )
        from Trip t
        join t.metrics tm
        where t.plannedStartDate >= :from
          and t.plannedStartDate <= :to
    """)
    TripAggregateKpiDTO getAggregateKpis(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
