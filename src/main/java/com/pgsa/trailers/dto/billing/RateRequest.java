package com.pgsa.trailers.dto.billing;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RateRequest(
        Long customerId,
        Long vehicleTypeId,
        String destinationPattern,
        Long commodityId,
        Long scheduleId,
        @PositiveOrZero BigDecimal fixedAmount,
        @PositiveOrZero BigDecimal perKm,
        @PositiveOrZero BigDecimal perTon,
        @PositiveOrZero BigDecimal perDay,
        @PositiveOrZero BigDecimal labourHourlyRate,
        @PositiveOrZero BigDecimal craneHourlyRate,
        Integer priority,
        @NotNull LocalDate validFrom,
        @NotNull LocalDate validTo) {
}
