package com.pgsa.trailers.repository.finance;

import com.pgsa.trailers.entity.finance.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    /**
     * All line items belonging to a specific invoice.
     */
    List<InvoiceItem> findByInvoiceId(Long invoiceId);

    /**
     * Load-linked line items — matches the polymorphic design:
     * {@code reference_type = 'LOAD'} AND {@code reference_id = <numeric id>}.
     *
     * <p>Replaces the legacy {@code findByLoadId(String)} which referenced a
     * column that no longer exists on {@link InvoiceItem}.
     */
    @Query("""
        SELECT ii FROM InvoiceItem ii
        WHERE ii.referenceType = 'LOAD'
          AND ii.referenceId = :referenceId
    """)
    List<InvoiceItem> findByLoadReferenceId(@Param("referenceId") Long referenceId);

    /**
     * Trip-linked line items — {@code reference_type = 'TRIP'} AND
     * {@code reference_id = <trip id>}.
     */
    @Query("""
        SELECT ii FROM InvoiceItem ii
        WHERE ii.referenceType = 'TRIP'
          AND ii.referenceId = :referenceId
    """)
    List<InvoiceItem> findByTripReferenceId(@Param("referenceId") Long referenceId);

    /**
     * Cross-type lookup by reference id regardless of type.
     * Useful for "show me every item linked to this numeric id".
     */
    @Query("""
        SELECT ii FROM InvoiceItem ii
        WHERE ii.referenceId = :referenceId
    """)
    List<InvoiceItem> findByReferenceId(@Param("referenceId") Long referenceId);

    /**
     * Filter by reference type alone — e.g. all trip items, all load items.
     */
    List<InvoiceItem> findByReferenceType(String referenceType);
}
