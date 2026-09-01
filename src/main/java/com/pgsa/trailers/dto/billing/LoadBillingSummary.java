package com.pgsa.trailers.dto.billing;

import com.pgsa.trailers.entity.billing.TripBilling;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class LoadBillingSummary {
    private String loadId;
    private String loadDescription;
    private Long customerId;
    private String customerName;
    private Integer totalTrips;
    private Long totalBillable;
    private Long totalInvoiced;
    private BigDecimal totalAmount;
    private BigDecimal subtotal;
    private BigDecimal vat;
    private List<TripBilling> trips;
    private Boolean canInvoice;
    private String status;
}
