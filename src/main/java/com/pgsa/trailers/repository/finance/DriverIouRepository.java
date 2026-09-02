package com.pgsa.trailers.repository.finance;

import com.pgsa.trailers.entity.finance.DriverIou;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverIouRepository extends JpaRepository<DriverIou, Long> {

    @Query("SELECT d FROM DriverIou d WHERE d.iouNumber = :iouNumber")
    Optional<DriverIou> findByIouNumber(@Param("iouNumber") String iouNumber);

    @Query("SELECT d FROM DriverIou d WHERE d.driverId = :driverId")
    List<DriverIou> findByDriverId(@Param("driverId") Long driverId);

    @Query("SELECT d FROM DriverIou d WHERE d.status = :status")
    List<DriverIou> findByStatus(@Param("status") String status);

    @Query("SELECT d FROM DriverIou d WHERE d.iouType = :iouType")
    List<DriverIou> findByIouType(@Param("iouType") String iouType);

    @Query("SELECT d FROM DriverIou d WHERE d.dueDate < :date AND d.status NOT IN ('PAID', 'CANCELLED')")
    List<DriverIou> findOverdueIous(@Param("date") LocalDate date);

    @Query("SELECT d FROM DriverIou d WHERE d.tripId = :tripId")
    List<DriverIou> findByTripId(@Param("tripId") Long tripId);

    @Query("SELECT d FROM DriverIou d WHERE d.deductedFromSalary = true AND d.deductionDate IS NULL")
    List<DriverIou> findPendingSalaryDeductions();

    @Query("SELECT SUM(d.balanceDue) FROM DriverIou d WHERE d.status = 'PENDING'")
    java.math.BigDecimal sumPendingBalance();

    @Query("SELECT SUM(d.balanceDue) FROM DriverIou d WHERE d.status = 'OVERDUE'")
    java.math.BigDecimal sumOverdueBalance();
}
