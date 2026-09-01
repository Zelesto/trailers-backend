package com.pgsa.trailers.repository.billing;

import com.pgsa.trailers.entity.billing.TripBilling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TripBillingRepository extends JpaRepository<TripBilling, Long> {

    @Query("SELECT tb FROM TripBilling tb WHERE tb.trip.id = :tripId")
    TripBilling findByTripId(@Param("tripId") Long tripId);

    @Query("SELECT tb FROM TripBilling tb WHERE tb.trip.loadId = :loadId")
    List<TripBilling> findByTrip_LoadId(@Param("loadId") String loadId);

    @Query("SELECT tb FROM TripBilling tb WHERE tb.customer.id = :customerId")
    List<TripBilling> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT tb FROM TripBilling tb WHERE tb.status = :status")
    List<TripBilling> findByStatus(@Param("status") String status);

    @Query("SELECT tb FROM TripBilling tb WHERE tb.invoiceId = :invoiceId")
    List<TripBilling> findByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TripBilling tb WHERE tb.trip.id = :tripId")
    void deleteByTripId(@Param("tripId") Long tripId);

    @Query("SELECT COUNT(tb) FROM TripBilling tb WHERE tb.status = 'CALCULATED'")
    long countCalculated();

    @Query("SELECT SUM(tb.total) FROM TripBilling tb WHERE tb.status = 'CALCULATED'")
    java.math.BigDecimal sumCalculatedTotal();
}
