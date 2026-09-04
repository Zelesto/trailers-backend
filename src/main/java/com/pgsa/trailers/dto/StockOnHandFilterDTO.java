package com.pgsa.trailers.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockOnHandFilterDTO {
    private String holderType; // VEHICLE, DRIVER, LOCATION, ALL
    private Long holderId;
    private Long itemId;
    private String category;
    private String search;
    private String status; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK, ON_HOLD
    private Boolean showOnlyOutstanding;
}
