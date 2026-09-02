package com.pgsa.trailers.repository.finance;

import com.pgsa.trailers.entity.finance.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("SELECT e FROM Expense e WHERE e.expenseNumber = :expenseNumber")
    Optional<Expense> findByExpenseNumber(@Param("expenseNumber") String expenseNumber);

    @Query("SELECT e FROM Expense e WHERE e.category = :category")
    List<Expense> findByCategory(@Param("category") String category);

    @Query("SELECT e FROM Expense e WHERE e.status = :status")
    List<Expense> findByStatus(@Param("status") String status);

    @Query("SELECT e FROM Expense e WHERE e.vendorName LIKE %:vendorName%")
    List<Expense> findByVendorNameContaining(@Param("vendorName") String vendorName);

    @Query("SELECT e FROM Expense e WHERE e.expenseDate BETWEEN :startDate AND :endDate")
    List<Expense> findByExpenseDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT e FROM Expense e WHERE e.vehicleId = :vehicleId")
    List<Expense> findByVehicleId(@Param("vehicleId") Long vehicleId);

    @Query("SELECT e FROM Expense e WHERE e.tripId = :tripId")
    List<Expense> findByTripId(@Param("tripId") Long tripId);

    @Query("SELECT e FROM Expense e WHERE e.driverId = :driverId")
    List<Expense> findByDriverId(@Param("driverId") Long driverId);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.category = :category AND e.expenseDate BETWEEN :startDate AND :endDate")
    java.math.BigDecimal sumByCategoryAndDateRange(
            @Param("category") String category,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT e.approvalStatus, COUNT(e) FROM Expense e GROUP BY e.approvalStatus")
    List<Object[]> countByApprovalStatus();

    @Query("SELECT e FROM Expense e WHERE e.approvalStatus = 'PENDING'")
    List<Expense> findPendingApproval();
}
