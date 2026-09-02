package com.pgsa.trailers.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ReceivableSummaryDTO {
    private Long totalReceivables;
    private BigDecimal totalAmount;
    private BigDecimal totalPaid;
    private BigDecimal totalOutstanding;
    private BigDecimal overdueAmount;
    private Long overdueCount;
}
