package com.pgsa.trailers.repository.billing;

import com.pgsa.trailers.entity.billing.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    @Query("SELECT COUNT(h) > 0 FROM Holiday h WHERE h.holidayDate = :date")
    boolean existsByHolidayDate(@Param("date") LocalDate date);

    @Query("SELECT h FROM Holiday h WHERE h.holidayDate BETWEEN :startDate AND :endDate")
    List<Holiday> findBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT h FROM Holiday h WHERE h.country = :country AND h.holidayDate >= :date")
    List<Holiday> findUpcomingByCountry(@Param("country") String country, @Param("date") LocalDate date);

    @Query("SELECT h FROM Holiday h WHERE h.isRecurring = true")
    List<Holiday> findRecurringHolidays();

    @Query("SELECT h FROM Holiday h WHERE h.holidayDate = :date AND (h.country = :country OR h.country IS NULL)")
    List<Holiday> findByDateAndCountry(@Param("date") LocalDate date, @Param("country") String country);
}
