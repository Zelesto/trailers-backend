package com.pgsa.trailers.service.ops;

import com.pgsa.trailers.entity.ops.WorkOrder;
import com.pgsa.trailers.entity.ops.WorkOrderProof;
import com.pgsa.trailers.entity.ops.WorkOrderStatus;
import com.pgsa.trailers.repository.ops.SowTemplateRepository;
import com.pgsa.trailers.repository.ops.WorkOrderProofRepository;
import com.pgsa.trailers.repository.ops.WorkOrderRepository;
import com.pgsa.trailers.service.SupabaseStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private WorkOrderProofRepository proofRepository;

    @Mock
    private SowTemplateRepository templateRepository;

    @Mock
    private SupabaseStorageService storageService;

    @InjectMocks
    private WorkOrderService workOrderService;

    @Test
    void cannotDeleteProofAfterWorkOrderIsCompleted() {
        WorkOrder workOrder = workOrder(WorkOrderStatus.COMPLETED);
        WorkOrderProof proof = new WorkOrderProof();
        proof.setId(7L);
        proof.setWorkOrder(workOrder);
        workOrder.getProofs().add(proof);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(workOrder));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> workOrderService.deleteProof(1L, 7L));

        assertEquals(409, error.getStatusCode().value());
        verify(storageService, never()).deleteFile(proof.getFileUrl());
        verify(proofRepository, never()).delete(proof);
    }

    @Test
    void cannotCompleteWorkOrderWithoutProof() {
        WorkOrder workOrder = workOrder(WorkOrderStatus.IN_PROGRESS);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(workOrder));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> workOrderService.updateStatus(1L, WorkOrderStatus.COMPLETED));

        assertEquals(400, error.getStatusCode().value());
        verify(workOrderRepository, never()).save(workOrder);
    }

    @Test
    void cannotAddProofAfterWorkOrderIsCompleted() {
        WorkOrder workOrder = workOrder(WorkOrderStatus.COMPLETED);
        when(workOrderRepository.findById(1L)).thenReturn(Optional.of(workOrder));
        MockMultipartFile file = new MockMultipartFile("file", "proof.pdf", "application/pdf", new byte[] {1});

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> workOrderService.addProof(1L, null, file));

        assertEquals(409, error.getStatusCode().value());
        verifyNoInteractions(storageService);
    }

    private WorkOrder workOrder(WorkOrderStatus status) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(1L);
        workOrder.setWorkOrderNumber("WO-TEST");
        workOrder.setTitle("Test work order");
        workOrder.setScopeOfWork("Test scope");
        workOrder.setStatus(status);
        return workOrder;
    }
}
