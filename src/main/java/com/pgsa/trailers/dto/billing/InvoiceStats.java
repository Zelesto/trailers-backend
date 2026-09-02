package com.pgsa.trailers.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class InvoiceStats {
    @Builder.Default
    private Long totalInvoices = 0L;
    
    @Builder.Default
    private Long overdueCount = 0L;
    
    @Builder.Default
    private BigDecimal totalPaid = BigDecimal.ZERO;
    
    @Builder.Default
    private BigDecimal totalOutstanding = BigDecimal.ZERO;
    
    private Long draftCount;
    private Long pendingCount;
    private Long sentCount;
    private Long paidCount;
}
