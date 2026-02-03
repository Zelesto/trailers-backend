package com.pgsa.trailers.repository;

import com.pgsa.trailers.dto.FuelFraudDTO;
import com.pgsa.trailers.entity.assets.Vehicle;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;


import java.util.List;


public interface FuelFraudRepository extends Repository<Vehicle, Long> {

    @Query(value = """
            select
                    v.registration_number,
            sum(tm.distance_actual / 2.5) as expectedLiters,
            sum(fs.liters) as actualLiters,
            sum(fs.liters) - sum(tm.distance_actual / 2.5) as variance
            from trip t
            join vehicle v on v.id = t.vehicle_id
            join trip_metrics tm on tm.trip_id = t.id
            join fuel_slip fs on fs.trip_id = t.id
            where t.status = 'FINALIZED'
            group by v.registration
            having abs(sum(fs.liters) - sum(tm.distance_actual / 2.5)) > 50
            """, nativeQuery = true)
            List<FuelFraudDTO> detectFuelAnomalies();
}
