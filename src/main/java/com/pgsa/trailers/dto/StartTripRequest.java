package com.pgsa.trailers.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record StartTripRequest(
        @NotNull
        @Positive
        BigDecimal actualStartOdometer
) {}
