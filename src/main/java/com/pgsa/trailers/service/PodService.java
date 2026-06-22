// src/main/java/com/pgsa/trailers/service/PodService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.PodRequestDTO;
import com.pgsa.trailers.dto.PodResponseDTO;
import com.pgsa.trailers.dto.PodStatistics;
import com.pgsa.trailers.entity.ops.Pod;
import com.pgsa.trailers.repository.PodRepository;
import com.pgsa.trailers.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PodService {

    private final PodRepository podRepository;
    private final TripRepository tripRepository;

    public PodResponseDTO createPod(PodRequestDTO request) {
        Pod pod = Pod.builder()
                .tripId(request.getTripId())
                .customerName(request.getCustomerName())
                .deliveryDate(request.getDeliveryDate())
                .status(request.getStatus() != null ? request.getStatus() : "PENDING")
                .documentType(request.getDocumentType())
                .fileSize(request.getFileSize())
                .fileUrl(request.getFileUrl())
                .fileName(request.getFileName())
                .notes(request.getNotes())
                .uploadedBy(request.getUploadedBy())
                .uploadedAt(LocalDateTime.now())
                .build();

        Pod saved = podRepository.save(pod);
        log.info("POD created with ID: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PodResponseDTO getPodById(Long id) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));
        return mapToResponse(pod);
    }

    @Transactional(readOnly = true)
    public PodResponseDTO getPodByNumber(String podNumber) {
        Pod pod = podRepository.findByPodNumber(podNumber)
                .orElseThrow(() -> new RuntimeException("POD not found with number: " + podNumber));
        return mapToResponse(pod);
    }

    @Transactional(readOnly = true)
    public List<PodResponseDTO> getPodsByTrip(Long tripId) {
        return podRepository.findByTripId(tripId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PodResponseDTO> getPodsByTripPaginated(Long tripId, Pageable pageable) {
        return podRepository.findByTripId(tripId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<PodResponseDTO> getAllPods(Pageable pageable) {
        return podRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<PodResponseDTO> searchPods(String searchTerm, Pageable pageable) {
        return podRepository.searchPods(searchTerm, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<PodResponseDTO> getPodsByStatus(String status, Pageable pageable) {
        return podRepository.findByStatus(status, pageable)
                .map(this::mapToResponse);
    }

    public PodResponseDTO updatePod(Long id, PodRequestDTO request) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));

        pod.setTripId(request.getTripId());
        pod.setCustomerName(request.getCustomerName());
        pod.setDeliveryDate(request.getDeliveryDate());
        pod.setStatus(request.getStatus());
        pod.setDocumentType(request.getDocumentType());
        pod.setFileSize(request.getFileSize());
        pod.setFileUrl(request.getFileUrl());
        pod.setFileName(request.getFileName());
        pod.setNotes(request.getNotes());

        Pod updated = podRepository.save(pod);
        log.info("POD updated with ID: {}", updated.getId());
        return mapToResponse(updated);
    }

    public PodResponseDTO updatePodStatus(Long id, String status) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));

        pod.setStatus(status);
        Pod updated = podRepository.save(pod);
        log.info("POD {} status updated to: {}", id, status);
        return mapToResponse(updated);
    }

    public PodResponseDTO verifyPod(Long id, String verifiedBy) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));

        pod.setStatus("VERIFIED");
        pod.setVerifiedBy(verifiedBy);
        pod.setVerifiedAt(LocalDateTime.now());
        Pod updated = podRepository.save(pod);
        log.info("POD {} verified by: {}", id, verifiedBy);
        return mapToResponse(updated);
    }

    public PodResponseDTO rejectPod(Long id, String rejectedBy, String reason) {
        Pod pod = podRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("POD not found with ID: " + id));

        pod.setStatus("REJECTED");
        pod.setRejectedBy(rejectedBy);
        pod.setRejectedAt(LocalDateTime.now());
        pod.setRejectionReason(reason);
        Pod updated = podRepository.save(pod);
        log.info("POD {} rejected by: {}, reason: {}", id, rejectedBy, reason);
        return mapToResponse(updated);
    }

    public void deletePod(Long id) {
        if (!podRepository.existsById(id)) {
            throw new RuntimeException("POD not found with ID: " + id);
        }
        podRepository.deleteById(id);
        log.info("POD deleted with ID: {}", id);
    }

    public PodStatistics getPodStatistics() {
        long total = podRepository.count();
        long pending = podRepository.countByStatus("PENDING");
        long delivered = podRepository.countByStatus("DELIVERED");
        long verified = podRepository.countByStatus("VERIFIED");
        long rejected = podRepository.countByStatus("REJECTED");

        return PodStatistics.builder()
                .total(total)
                .pending(pending)
                .delivered(delivered)
                .verified(verified)
                .rejected(rejected)
                .build();
    }

    /**
     * Helper method to get trip number from trip ID
     */
    private String getTripNumber(Long tripId) {
        if (tripId == null) {
            return null;
        }
        try {
            // Use the repository method with Optional
            return tripRepository.findTripNumberById(tripId)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Could not find trip number for trip ID: {}", tripId, e);
            return null;
        }
    }

    private PodResponseDTO mapToResponse(Pod pod) {
        // Get trip number from trip ID
        String tripNumber = getTripNumber(pod.getTripId());

        return PodResponseDTO.builder()
                .id(pod.getId())
                .podNumber(pod.getPodNumber())
                .tripId(pod.getTripId())
                .tripNumber(tripNumber)  // Now the builder has this method
                .customerName(pod.getCustomerName())
                .deliveryDate(pod.getDeliveryDate())
                .status(pod.getStatus())
                .documentType(pod.getDocumentType())
                .fileSize(pod.getFileSize())
                .fileUrl(pod.getFileUrl())
                .fileName(pod.getFileName())
                .notes(pod.getNotes())
                .uploadedBy(pod.getUploadedBy())
                .uploadedAt(pod.getUploadedAt())
                .verifiedBy(pod.getVerifiedBy())
                .verifiedAt(pod.getVerifiedAt())
                .rejectedBy(pod.getRejectedBy())
                .rejectedAt(pod.getRejectedAt())
                .rejectionReason(pod.getRejectionReason())
                .createdAt(pod.getCreatedAt())
                .createdBy(pod.getCreatedBy())
                .updatedAt(pod.getUpdatedAt())
                .updatedBy(pod.getUpdatedBy())
                .build();
    }
}
