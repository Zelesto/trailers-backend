package com.pgsa.trailers.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record EndTripRequest(
        @NotNull
        @Positive
        BigDecimal actualEndOdometer
) {}
