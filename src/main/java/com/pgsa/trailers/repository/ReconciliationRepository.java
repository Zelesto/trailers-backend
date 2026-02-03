package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.finance.Reconciliation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationRepository extends JpaRepository<Reconciliation, Long> {
    // You can add custom queries if needed later
}
