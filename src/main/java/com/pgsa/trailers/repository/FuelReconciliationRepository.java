package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.finance.Reconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FuelReconciliationRepository extends JpaRepository<Reconciliation, Long> {

    @Query(value = """
            select
                a.name as accountName,
                coalesce(sum(fs.total_amount), 0) as slipsTotal,
                coalesce(sum(p.amount), 0) as paymentsTotal,
                coalesce(sum(p.amount), 0) - coalesce(sum(fs.total_amount), 0) as variance
            from account a
            left join fuel_source fsr on fsr.account_id = a.id
            left join fuel_slip fs on fs.fuel_source_id = fsr.id
                and fs.transaction_date between :from and :to
            left join payment p on p.account_id = a.id
                and p.payment_date between :from and :to
            where a.type = 'FUEL'
            group by a.name
            """, nativeQuery = true)
    List<Object[]> reconcileFuelRaw(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}