package com.pgsa.trailers.dto.billing;

import com.pgsa.trailers.entity.billing.Rate;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RateResponse(
        Long id,
        Long customerId,
        String customerName,
        Long vehicleTypeId,
        String vehicleTypeName,
        String destinationPattern,
        Long commodityId,
        String commodityName,
        Long scheduleId,
        String scheduleName,
        BigDecimal fixedAmount,
        BigDecimal perKm,
        BigDecimal perTon,
        BigDecimal perDay,
        BigDecimal labourHourlyRate,
        BigDecimal craneHourlyRate,
        Integer priority,
        LocalDate validFrom,
        LocalDate validTo,
        Boolean isActive) {

    public static RateResponse from(Rate rate) {
        return new RateResponse(
                rate.getId(),
                rate.getCustomer() == null ? null : rate.getCustomer().getId(),
                rate.getCustomer() == null ? null : rate.getCustomer().getName(),
                rate.getVehicleType() == null ? null : rate.getVehicleType().getId(),
                rate.getVehicleType() == null ? null : rate.getVehicleType().getName(),
                rate.getDestinationPattern(),
                rate.getCommodity() == null ? null : rate.getCommodity().getId(),
                rate.getCommodity() == null ? null : rate.getCommodity().getName(),
                rate.getSchedule() == null ? null : rate.getSchedule().getId(),
                rate.getSchedule() == null ? null : rate.getSchedule().getName(),
                rate.getFixedAmount(),
                rate.getPerKm(),
                rate.getPerTon(),
                rate.getPerDay(),
                rate.getLabourHourlyRate(),
                rate.getCraneHourlyRate(),
                rate.getPriority(),
                rate.getValidFrom(),
                rate.getValidTo(),
                rate.getIsActive());
    }
}
