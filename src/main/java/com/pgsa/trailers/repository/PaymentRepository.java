package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.finance.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
            select sum(p.amount)
            from Payment p
            where p.account.id = :accountId
            and p.paymentDate between :from and :to
            """)
            BigDecimal sumPaymentsForAccount(
                    @Param("accountId") Long accountId,
                    @Param("from") LocalDate from,
                    @Param("to") LocalDate to
            );
}
