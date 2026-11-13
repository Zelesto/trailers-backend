package com.pgsa.trailers.dto.ops;

import jakarta.validation.constraints.NotBlank;

public record SowTemplateRequest(
        @NotBlank String name,
        String description,
        @NotBlank String scopeOfWork,
        String jobInstructions,
        String deliverables) {
}
