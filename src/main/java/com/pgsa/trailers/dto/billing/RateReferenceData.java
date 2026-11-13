package com.pgsa.trailers.dto.billing;

import java.util.List;

public record RateReferenceData(
        List<Option> customers,
        List<Option> vehicleTypes,
        List<Option> commodities,
        List<Option> schedules) {

    public record Option(Long id, String name) {
    }
}
