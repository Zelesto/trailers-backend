package com.pgsa.trailers.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class InvoiceStats {
    private Long totalInvoices;
    private Long overdueCount;
    private BigDecimal totalPaid;
    private BigDecimal totalOutstanding;
}
