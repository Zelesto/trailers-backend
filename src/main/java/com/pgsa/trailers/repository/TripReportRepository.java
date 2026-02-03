package com.pgsa.trailers.repository;

import com.pgsa.trailers.dto.TripCostReportDTO;
import com.pgsa.trailers.entity.ops.Trip;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;


import java.time.LocalDate;
import java.util.List;


public interface TripReportRepository extends Repository<Trip, Long> {

    @Query(value = """
            select
                    t.id as tripId,
            v.registration_number as vehicleReg,
            tm.total_distance as distanceKm,
            coalesce(sum(fs.total_amount),0) as fuelCost,
            0 as tollCost,
            0 as foodCost,
            0 as adverseCost,
            coalesce(sum(fs.total_amount),0) as totalCost
    from trip t
    join vehicle v on v.id = t.vehicle_id
    left join trip_metrics tm on tm.trip_id = t.id
    left join fuel_slip fs on fs.vehicle_id = v.id
    where t.start_date between :from and :to
    group by t.id, v.registration_number, tm.total_distance
    """, nativeQuery = true)
    List<TripCostReportDTO> tripCostReport(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}