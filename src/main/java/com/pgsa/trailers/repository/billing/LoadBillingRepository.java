package com.pgsa.trailers.repository.billing;

import com.pgsa.trailers.entity.billing.LoadBilling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoadBillingRepository extends JpaRepository<LoadBilling, Long> {

    @Query("SELECT lb FROM LoadBilling lb WHERE lb.loadId = :loadId")
    Optional<LoadBilling> findByLoadId(@Param("loadId") String loadId);

    @Query("SELECT lb FROM LoadBilling lb WHERE lb.customerId = :customerId")
    List<LoadBilling> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT lb FROM LoadBilling lb WHERE lb.status = :status")
    List<LoadBilling> findByStatus(@Param("status") String status);

    @Query("SELECT lb FROM LoadBilling lb WHERE lb.invoiceId = :invoiceId")
    Optional<LoadBilling> findByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query("SELECT lb FROM LoadBilling lb WHERE lb.status IN ('CALCULATED', 'DRAFT') AND lb.invoiceId IS NULL")
    List<LoadBilling> findBillableLoads();

    @Query("SELECT lb FROM LoadBilling lb WHERE lb.customerId = :customerId AND lb.status = 'INVOICED'")
    List<LoadBilling> findInvoicedByCustomer(@Param("customerId") Long customerId);

    @Query("SELECT SUM(lb.total) FROM LoadBilling lb WHERE lb.status = 'INVOICED'")
    java.math.BigDecimal sumInvoicedTotal();

    @Query("SELECT SUM(lb.total) FROM LoadBilling lb WHERE lb.status = 'CALCULATED' AND lb.invoiceId IS NULL")
    java.math.BigDecimal sumCalculatedTotal();
}
