package com.pgsa.trailers.dto.ops;

import com.pgsa.trailers.entity.ops.SowTemplate;

public record SowTemplateResponse(
        Long id,
        String name,
        String description,
        String scopeOfWork,
        String jobInstructions,
        String deliverables,
        Boolean isActive) {

    public static SowTemplateResponse from(SowTemplate template) {
        return new SowTemplateResponse(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getScopeOfWork(),
                template.getJobInstructions(),
                template.getDeliverables(),
                template.getIsActive());
    }
}
