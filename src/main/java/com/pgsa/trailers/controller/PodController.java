// src/main/java/com/pgsa/trailers/controller/PodController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.PodRequestDTO;
import com.pgsa.trailers.dto.PodResponseDTO;
import com.pgsa.trailers.service.PodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pods")
@RequiredArgsConstructor
public class PodController {

    private final PodService podService;

    @PostMapping
    public ResponseEntity<PodResponseDTO> createPod(@Valid @RequestBody PodRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(podService.createPod(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PodResponseDTO> getPodById(@PathVariable Long id) {
        return ResponseEntity.ok(podService.getPodById(id));
    }

    @GetMapping("/number/{podNumber}")
    public ResponseEntity<PodResponseDTO> getPodByNumber(@PathVariable String podNumber) {
        return ResponseEntity.ok(podService.getPodByNumber(podNumber));
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<PodResponseDTO>> getPodsByTrip(@PathVariable Long tripId) {
        return ResponseEntity.ok(podService.getPodsByTrip(tripId));
    }

    @GetMapping("/trip/{tripId}/page")
    public ResponseEntity<Page<PodResponseDTO>> getPodsByTripPaginated(
            @PathVariable Long tripId,
            @PageableDefault(size = 10, sort = "deliveryDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(podService.getPodsByTripPaginated(tripId, pageable));
    }

    @GetMapping
    public ResponseEntity<Page<PodResponseDTO>> getAllPods(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(podService.getAllPods(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PodResponseDTO>> searchPods(
            @RequestParam String q,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(podService.searchPods(q, pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<PodResponseDTO>> getPodsByStatus(
            @PathVariable String status,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(podService.getPodsByStatus(status, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PodResponseDTO> updatePod(
            @PathVariable Long id,
            @Valid @RequestBody PodRequestDTO request) {
        return ResponseEntity.ok(podService.updatePod(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PodResponseDTO> updatePodStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(podService.updatePodStatus(id, status));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<PodResponseDTO> verifyPod(
            @PathVariable Long id,
            @RequestParam String verifiedBy) {
        return ResponseEntity.ok(podService.verifyPod(id, verifiedBy));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<PodResponseDTO> rejectPod(
            @PathVariable Long id,
            @RequestParam String rejectedBy,
            @RequestParam String reason) {
        return ResponseEntity.ok(podService.rejectPod(id, rejectedBy, reason));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePod(@PathVariable Long id) {
        podService.deletePod(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistics")
    public ResponseEntity<PodStatistics> getPodStatistics() {
        return ResponseEntity.ok(podService.getPodStatistics());
    }
}
