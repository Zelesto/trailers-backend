package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.ops.SowTemplateActivationRequest;
import com.pgsa.trailers.dto.ops.SowTemplateRequest;
import com.pgsa.trailers.dto.ops.SowTemplateResponse;
import com.pgsa.trailers.dto.ops.WorkOrderProofResponse;
import com.pgsa.trailers.dto.ops.WorkOrderRequest;
import com.pgsa.trailers.dto.ops.WorkOrderResponse;
import com.pgsa.trailers.dto.ops.WorkOrderStatusRequest;
import com.pgsa.trailers.entity.ops.WorkOrderProofType;
import com.pgsa.trailers.entity.ops.WorkOrderStatus;
import com.pgsa.trailers.service.ops.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @GetMapping
    public List<WorkOrderResponse> list(@RequestParam(required = false) WorkOrderStatus status) {
        return workOrderService.list(status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkOrderResponse create(@Valid @RequestBody WorkOrderRequest request) {
        return workOrderService.create(request);
    }

    @PutMapping("/{id}")
    public WorkOrderResponse update(@PathVariable Long id, @Valid @RequestBody WorkOrderRequest request) {
        return workOrderService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public WorkOrderResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody WorkOrderStatusRequest request) {
        return workOrderService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        workOrderService.delete(id);
    }

    @PostMapping(value = "/{id}/proof", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkOrderProofResponse addProof(
            @PathVariable Long id,
            @RequestParam WorkOrderProofType proofType,
            @RequestParam MultipartFile file) {
        return workOrderService.addProof(id, proofType, file);
    }

    @DeleteMapping("/{id}/proof/{proofId}")
    public WorkOrderResponse deleteProof(@PathVariable Long id, @PathVariable Long proofId) {
        return workOrderService.deleteProof(id, proofId);
    }

    @GetMapping("/templates")
    public List<SowTemplateResponse> listTemplates(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return workOrderService.listTemplates(includeInactive);
    }

    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public SowTemplateResponse createTemplate(@Valid @RequestBody SowTemplateRequest request) {
        return workOrderService.createTemplate(request);
    }

    @PutMapping("/templates/{id}")
    public SowTemplateResponse updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody SowTemplateRequest request) {
        return workOrderService.updateTemplate(id, request);
    }

    @PatchMapping("/templates/{id}/active")
    public SowTemplateResponse setTemplateActive(
            @PathVariable Long id,
            @Valid @RequestBody SowTemplateActivationRequest request) {
        return workOrderService.setTemplateActive(id, request.active());
    }
}
