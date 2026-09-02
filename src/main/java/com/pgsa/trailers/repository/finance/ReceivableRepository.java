package com.pgsa.trailers.repository.finance;

import com.pgsa.trailers.entity.finance.Receivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceivableRepository extends JpaRepository<Receivable, Long> {

    @Query("SELECT r FROM Receivable r WHERE r.receivableNumber = :receivableNumber")
    Optional<Receivable> findByReceivableNumber(@Param("receivableNumber") String receivableNumber);

    @Query("SELECT r FROM Receivable r WHERE r.customerId = :customerId")
    List<Receivable> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT r FROM Receivable r WHERE r.driverId = :driverId")
    List<Receivable> findByDriverId(@Param("driverId") Long driverId);

    @Query("SELECT r FROM Receivable r WHERE r.status = :status")
    List<Receivable> findByStatus(@Param("status") String status);

    @Query("SELECT r FROM Receivable r WHERE r.invoiceId = :invoiceId")
    List<Receivable> findByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query("SELECT r FROM Receivable r WHERE r.dueDate < :date AND r.status NOT IN ('PAID', 'CANCELLED')")
    List<Receivable> findOverdueReceivables(@Param("date") LocalDate date);

    @Query("SELECT r FROM Receivable r WHERE r.dueDate BETWEEN :startDate AND :endDate")
    List<Receivable> findReceivablesDueBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT SUM(r.balanceDue) FROM Receivable r WHERE r.status = 'PENDING'")
    java.math.BigDecimal sumPendingBalance();

    @Query("SELECT SUM(r.balanceDue) FROM Receivable r WHERE r.status = 'OVERDUE'")
    java.math.BigDecimal sumOverdueBalance();
}
