package com.pgsa.trailers.repository.finance;

import com.pgsa.trailers.entity.finance.Payable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayableRepository extends JpaRepository<Payable, Long> {

    @Query("SELECT p FROM Payable p WHERE p.payableNumber = :payableNumber")
    Optional<Payable> findByPayableNumber(@Param("payableNumber") String payableNumber);

    @Query("SELECT p FROM Payable p WHERE p.supplierId = :supplierId")
    List<Payable> findBySupplierId(@Param("supplierId") Long supplierId);

    @Query("SELECT p FROM Payable p WHERE p.status = :status")
    List<Payable> findByStatus(@Param("status") String status);

    @Query("SELECT p FROM Payable p WHERE p.category = :category")
    List<Payable> findByCategory(@Param("category") String category);

    @Query("SELECT p FROM Payable p WHERE p.dueDate < :date AND p.status NOT IN ('PAID', 'CANCELLED')")
    List<Payable> findOverduePayables(@Param("date") LocalDate date);

    @Query("SELECT p FROM Payable p WHERE p.dueDate BETWEEN :startDate AND :endDate")
    List<Payable> findPayablesDueBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT SUM(p.balanceDue) FROM Payable p WHERE p.status = 'PENDING'")
    java.math.BigDecimal sumPendingBalance();

    @Query("SELECT SUM(p.balanceDue) FROM Payable p WHERE p.status = 'OVERDUE'")
    java.math.BigDecimal sumOverdueBalance();

    @Query("SELECT p FROM Payable p WHERE p.approvalStatus = 'PENDING'")
    List<Payable> findPendingApproval();

    @Query("SELECT p FROM Payable p WHERE p.approvalStatus = 'APPROVED' AND p.status = 'PENDING'")
    List<Payable> findApprovedPendingPayment();
}
