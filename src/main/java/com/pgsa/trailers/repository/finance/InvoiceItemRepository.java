package com.pgsa.trailers.repository.finance;

import com.pgsa.trailers.entity.finance.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    List<InvoiceItem> findByInvoiceId(Long invoiceId);

    @Query("SELECT ii FROM InvoiceItem ii WHERE ii.tripId = :tripId")
    List<InvoiceItem> findByTripId(@Param("tripId") Long tripId);

    @Query("SELECT ii FROM InvoiceItem ii WHERE ii.loadId = :loadId")
    List<InvoiceItem> findByLoadId(@Param("loadId") String loadId);

    @Query("SELECT SUM(ii.lineTotal) FROM InvoiceItem ii WHERE ii.invoice.id = :invoiceId")
    java.math.BigDecimal sumLineTotalByInvoiceId(@Param("invoiceId") Long invoiceId);
}
