package com.pgsa.trailers.dto.billing;

import jakarta.validation.constraints.NotNull;

public record RateActivationRequest(@NotNull Boolean active) {
}
