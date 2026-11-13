package com.pgsa.trailers.repository.billing;

import com.pgsa.trailers.entity.billing.RateSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RateScheduleRepository extends JpaRepository<RateSchedule, Long> {
    List<RateSchedule> findByIsActiveTrueOrderByNameAsc();
}
