package com.pgsa.trailers.service.ops;

import com.pgsa.trailers.dto.ops.SowTemplateRequest;
import com.pgsa.trailers.dto.ops.SowTemplateResponse;
import com.pgsa.trailers.dto.ops.WorkOrderRequest;
import com.pgsa.trailers.dto.ops.WorkOrderResponse;
import com.pgsa.trailers.dto.ops.WorkOrderProofResponse;
import com.pgsa.trailers.entity.ops.SowTemplate;
import com.pgsa.trailers.entity.ops.WorkOrder;
import com.pgsa.trailers.entity.ops.WorkOrderProof;
import com.pgsa.trailers.entity.ops.WorkOrderProofType;
import com.pgsa.trailers.entity.ops.WorkOrderStatus;
import com.pgsa.trailers.repository.ops.SowTemplateRepository;
import com.pgsa.trailers.repository.ops.WorkOrderProofRepository;
import com.pgsa.trailers.repository.ops.WorkOrderRepository;
import com.pgsa.trailers.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderProofRepository proofRepository;
    private final SowTemplateRepository templateRepository;
    private final SupabaseStorageService storageService;

    @Transactional(readOnly = true)
    public List<WorkOrderResponse> list(WorkOrderStatus status) {
        List<WorkOrder> workOrders = status == null
                ? workOrderRepository.findAllByOrderByCreatedAtDesc()
                : workOrderRepository.findByStatusOrderByCreatedAtDesc(status);
        return workOrders.stream().map(WorkOrderResponse::from).toList();
    }

    @Transactional
    public WorkOrderResponse create(WorkOrderRequest request) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWorkOrderNumber("WO-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT));
        workOrder.setStatus(WorkOrderStatus.PENDING);
        apply(workOrder, request);
        return WorkOrderResponse.from(workOrderRepository.save(workOrder));
    }

    @Transactional
    public WorkOrderResponse update(Long id, WorkOrderRequest request) {
        WorkOrder workOrder = findWorkOrder(id);
        if (workOrder.getStatus() == WorkOrderStatus.COMPLETED || workOrder.getStatus() == WorkOrderStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Completed work orders cannot be edited");
        }
        apply(workOrder, request);
        return WorkOrderResponse.from(workOrderRepository.save(workOrder));
    }

    @Transactional
    public WorkOrderResponse updateStatus(Long id, WorkOrderStatus status) {
        WorkOrder workOrder = findWorkOrder(id);
        WorkOrderStatus expected = switch (workOrder.getStatus()) {
            case PENDING -> WorkOrderStatus.IN_PROGRESS;
            case IN_PROGRESS -> WorkOrderStatus.COMPLETED;
            case COMPLETED -> WorkOrderStatus.APPROVED;
            case APPROVED -> null;
        };
        if (status != expected) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Work order status must progress from " + workOrder.getStatus() + " to " + expected);
        }
        if ((status == WorkOrderStatus.COMPLETED || status == WorkOrderStatus.APPROVED)
                && workOrder.getProofs().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add proof of completion before completing the work order");
        }
        workOrder.setStatus(status);
        return WorkOrderResponse.from(workOrderRepository.save(workOrder));
    }

    @Transactional
    public void delete(Long id) {
        WorkOrder workOrder = findWorkOrder(id);
        if (workOrder.getStatus() != WorkOrderStatus.PENDING || !workOrder.getProofs().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending work orders without proof can be deleted");
        }
        workOrderRepository.delete(workOrder);
    }

    @Transactional
    public WorkOrderProofResponse addProof(Long id, WorkOrderProofType proofType, MultipartFile file) {
        WorkOrder workOrder = findWorkOrder(id);
        if (workOrder.getStatus() == WorkOrderStatus.COMPLETED
                || workOrder.getStatus() == WorkOrderStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Proof cannot be changed after a work order is completed");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A proof file is required");
        }
        if (proofType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Proof type is required");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The proof file must have a filename");
        }

        String fileUrl = storageService.uploadFile(
                file,
                workOrder.getWorkOrderNumber() + "_" + UUID.randomUUID().toString().substring(0, 8));
        WorkOrderProof proof = new WorkOrderProof();
        proof.setWorkOrder(workOrder);
        proof.setProofType(proofType);
        proof.setFileName(fileName);
        proof.setContentType(file.getContentType());
        proof.setFileUrl(fileUrl);
        WorkOrderProof saved = proofRepository.save(proof);
        workOrder.getProofs().add(saved);
        return WorkOrderProofResponse.from(saved);
    }

    @Transactional
    public WorkOrderResponse deleteProof(Long id, Long proofId) {
        WorkOrder workOrder = findWorkOrder(id);
        if (workOrder.getStatus() == WorkOrderStatus.COMPLETED
                || workOrder.getStatus() == WorkOrderStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Proof cannot be removed after a work order is completed");
        }
        WorkOrderProof proof = workOrder.getProofs().stream()
                .filter(item -> item.getId().equals(proofId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proof not found"));
        storageService.deleteFile(proof.getFileUrl());
        workOrder.getProofs().remove(proof);
        proofRepository.delete(proof);
        return WorkOrderResponse.from(workOrderRepository.save(workOrder));
    }

    @Transactional(readOnly = true)
    public List<SowTemplateResponse> listTemplates(boolean includeInactive) {
        List<SowTemplate> templates = includeInactive
                ? templateRepository.findAllByOrderByNameAsc()
                : templateRepository.findByIsActiveTrueOrderByNameAsc();
        return templates.stream().map(SowTemplateResponse::from).toList();
    }

    @Transactional
    public SowTemplateResponse createTemplate(SowTemplateRequest request) {
        SowTemplate template = new SowTemplate();
        apply(template, request);
        template.setIsActive(true);
        return SowTemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public SowTemplateResponse updateTemplate(Long id, SowTemplateRequest request) {
        SowTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SOW template not found"));
        apply(template, request);
        return SowTemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public SowTemplateResponse setTemplateActive(Long id, boolean active) {
        SowTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SOW template not found"));
        template.setIsActive(active);
        return SowTemplateResponse.from(templateRepository.save(template));
    }

    private WorkOrder findWorkOrder(Long id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work order not found"));
    }

    private void apply(WorkOrder workOrder, WorkOrderRequest request) {
        if (request.dueDate() != null && request.dueDate().isBefore(java.time.LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date cannot be in the past");
        }
        if (request.sowTemplateId() != null) {
            SowTemplate template = templateRepository.findById(request.sowTemplateId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "SOW template not found"));
            if (!Boolean.TRUE.equals(template.getIsActive())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The selected SOW template is inactive");
            }
        }
        workOrder.setTitle(request.title().trim());
        workOrder.setCustomerId(request.customerId());
        workOrder.setScopeOfWork(request.scopeOfWork().trim());
        workOrder.setJobInstructions(blankToNull(request.jobInstructions()));
        workOrder.setDeliverables(blankToNull(request.deliverables()));
        workOrder.setAssignedDriverId(request.assignedDriverId());
        workOrder.setAssignedVehicleId(request.assignedVehicleId());
        workOrder.setTeamName(blankToNull(request.teamName()));
        workOrder.setDueDate(request.dueDate());
        workOrder.setSowTemplateId(request.sowTemplateId());
    }

    private void apply(SowTemplate template, SowTemplateRequest request) {
        template.setName(request.name().trim());
        template.setDescription(blankToNull(request.description()));
        template.setScopeOfWork(request.scopeOfWork().trim());
        template.setJobInstructions(blankToNull(request.jobInstructions()));
        template.setDeliverables(blankToNull(request.deliverables()));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
