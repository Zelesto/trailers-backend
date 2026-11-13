package com.pgsa.trailers.dto.ops;

import jakarta.validation.constraints.NotNull;

public record SowTemplateActivationRequest(@NotNull Boolean active) {
}
