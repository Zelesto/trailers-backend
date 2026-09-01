package com.pgsa.trailers.repository.finance;

import com.pgsa.trailers.entity.finance.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT i FROM Invoice i WHERE i.invoiceNumber = :invoiceNumber")
    Optional<Invoice> findByInvoiceNumber(@Param("invoiceNumber") String invoiceNumber);

    @Query("SELECT i FROM Invoice i WHERE i.customerId = :customerId")
    List<Invoice> findByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT i FROM Invoice i WHERE i.status = :status")
    List<Invoice> findByStatus(@Param("status") String status);

    @Query("SELECT i FROM Invoice i WHERE i.invoiceType = :invoiceType")
    List<Invoice> findByInvoiceType(@Param("invoiceType") String invoiceType);

    @Query("SELECT i FROM Invoice i WHERE i.source = :source")
    List<Invoice> findBySource(@Param("source") String source);

    @Query("SELECT i FROM Invoice i WHERE i.dueDate < :now AND i.status != 'PAID' AND i.status != 'CANCELLED'")
    List<Invoice> findOverdueInvoices(@Param("now") LocalDateTime now);

    @Query("SELECT i FROM Invoice i WHERE i.referenceId = :referenceId")
    List<Invoice> findByReferenceId(@Param("referenceId") String referenceId);

    @Query("SELECT i FROM Invoice i WHERE " +
           "(:customerId IS NULL OR i.customerId = :customerId) AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:invoiceType IS NULL OR i.invoiceType = :invoiceType) AND " +
           "(:source IS NULL OR i.source = :source)")
    List<Invoice> findWithFilters(
        @Param("customerId") Long customerId,
        @Param("status") String status,
        @Param("invoiceType") String invoiceType,
        @Param("source") String source
    );

    @Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.status = 'PAID'")
    java.math.BigDecimal sumPaidAmount();

    @Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.status != 'PAID' AND i.status != 'CANCELLED'")
    java.math.BigDecimal sumOutstandingAmount();

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.status = 'OVERDUE'")
    long countOverdue();
}
