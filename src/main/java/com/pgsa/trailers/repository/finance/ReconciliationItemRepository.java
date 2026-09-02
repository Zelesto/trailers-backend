package com.pgsa.trailers.repository.finance;

import com.pgsa.trailers.entity.finance.ReconciliationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReconciliationItemRepository extends JpaRepository<ReconciliationItem, Long> {

    @Query("SELECT ri FROM ReconciliationItem ri WHERE ri.reconciliationId = :reconciliationId")
    List<ReconciliationItem> findByReconciliationId(@Param("reconciliationId") Long reconciliationId);

    @Query("SELECT ri FROM ReconciliationItem ri WHERE ri.transactionId = :transactionId")
    List<ReconciliationItem> findByTransactionId(@Param("transactionId") Long transactionId);

    @Query("SELECT ri FROM ReconciliationItem ri WHERE ri.matched = false AND ri.reconciliationId = :reconciliationId")
    List<ReconciliationItem> findUnmatchedByReconciliationId(@Param("reconciliationId") Long reconciliationId);

    @Query("SELECT COUNT(ri) FROM ReconciliationItem ri WHERE ri.matched = true AND ri.reconciliationId = :reconciliationId")
    long countMatchedByReconciliationId(@Param("reconciliationId") Long reconciliationId);

    @Query("SELECT SUM(ri.statementAmount) FROM ReconciliationItem ri WHERE ri.matched = true AND ri.reconciliationId = :reconciliationId")
    java.math.BigDecimal sumMatchedAmountByReconciliationId(@Param("reconciliationId") Long reconciliationId);
}
