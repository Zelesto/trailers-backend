package com.pgsa.trailers.repository.finance;

import com.pgsa.trailers.entity.finance.Reconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReconciliationRepository extends JpaRepository<Reconciliation, Long> {

    // ❌ REMOVE THIS - column doesn't exist in database
    // Optional<Reconciliation> findByReconciliationNumber(String reconciliationNumber);

    @Query("SELECT r FROM Reconciliation r WHERE r.accountId = :accountId")
    List<Reconciliation> findByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT r FROM Reconciliation r WHERE r.status = :status")
    List<Reconciliation> findByStatus(@Param("status") String status);

    @Query("SELECT r FROM Reconciliation r WHERE r.accountId = :accountId AND r.status = 'COMPLETED'")
    List<Reconciliation> findCompletedByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT r FROM Reconciliation r WHERE r.accountId = :accountId AND r.reconciliationDate = :date")
    Optional<Reconciliation> findByAccountIdAndDate(
            @Param("accountId") Long accountId,
            @Param("date") LocalDate date
    );

    @Query("SELECT r FROM Reconciliation r WHERE r.reconciliationDate BETWEEN :startDate AND :endDate")
    List<Reconciliation> findByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT r FROM Reconciliation r WHERE r.accountId = :accountId AND r.reconciled = false")
    List<Reconciliation> findUnreconciledByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT COUNT(r) FROM Reconciliation r WHERE r.status = 'IN_PROGRESS'")
    long countInProgress();
}
