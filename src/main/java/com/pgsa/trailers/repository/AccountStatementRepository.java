package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.finance.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AccountStatementRepository
        extends JpaRepository<AccountStatement, Long> {

    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
        FROM AccountStatement a
        WHERE a.account.id = :accountId
          AND a.periodStart = :periodStart
          AND a.periodEnd = :periodEnd
    """)
    boolean existsForAccountAndPeriod(
            @Param("accountId") Long accountId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );
}
