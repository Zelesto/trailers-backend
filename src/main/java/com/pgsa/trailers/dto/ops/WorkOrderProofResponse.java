package com.pgsa.trailers.dto.ops;

import com.pgsa.trailers.entity.ops.WorkOrderProof;
import com.pgsa.trailers.entity.ops.WorkOrderProofType;

import java.time.LocalDateTime;

public record WorkOrderProofResponse(
        Long id,
        WorkOrderProofType proofType,
        String fileName,
        String contentType,
        String fileUrl,
        LocalDateTime uploadedAt) {

    public static WorkOrderProofResponse from(WorkOrderProof proof) {
        return new WorkOrderProofResponse(
                proof.getId(),
                proof.getProofType(),
                proof.getFileName(),
                proof.getContentType(),
                proof.getFileUrl(),
                proof.getUploadedAt());
    }
}
