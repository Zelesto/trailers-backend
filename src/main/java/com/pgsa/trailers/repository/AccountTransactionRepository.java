package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.finance.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    // Corrected method name
    List<AccountTransaction> findByAccount_Id(Long accountId);

    List<AccountTransaction> findByAccountIdAndReconciledFalse(Long accountId);

    @Query("SELECT COALESCE(SUM(a.amount), 0) " +
            "FROM AccountTransaction a " +
            "WHERE a.account.id = :accountId " +
            "AND a.direction = :direction " +
            "AND a.transactionDate BETWEEN :start AND :end")
    BigDecimal sumByAccountAndDirectionAndDateRange(
            @Param("accountId") Long accountId,
            @Param("direction") String direction,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
