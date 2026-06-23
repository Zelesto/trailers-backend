// src/main/java/com/pgsa/trailers/controller/LoadController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.LoadRequestDTO;
import com.pgsa.trailers.dto.LoadResponseDTO;
import com.pgsa.trailers.service.LoadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loads")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
public class LoadController {

    private final LoadService loadService;

    @PostMapping
    public ResponseEntity<LoadResponseDTO> createLoad(@Valid @RequestBody LoadRequestDTO request) {
        log.info("Creating new load: {}", request.getLoadNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(loadService.createLoad(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LoadResponseDTO> updateLoad(
            @PathVariable Long id,
            @Valid @RequestBody LoadRequestDTO request) {
        log.info("Updating load with ID: {}", id);
        return ResponseEntity.ok(loadService.updateLoad(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoadResponseDTO> getLoadById(@PathVariable Long id) {
        log.info("Fetching load with ID: {}", id);
        return ResponseEntity.ok(loadService.getLoadById(id));
    }

    @GetMapping("/number/{loadNumber}")
    public ResponseEntity<LoadResponseDTO> getLoadByNumber(@PathVariable String loadNumber) {
        log.info("Fetching load with number: {}", loadNumber);
        return ResponseEntity.ok(loadService.getLoadByNumber(loadNumber));
    }

    @GetMapping
    public ResponseEntity<Page<LoadResponseDTO>> getAllLoads(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching all loads");
        return ResponseEntity.ok(loadService.getAllLoads(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<LoadResponseDTO>> searchLoads(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Searching loads with term: {}", search);
        return ResponseEntity.ok(loadService.searchLoads(search, pageable));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<LoadResponseDTO>> getLoadsByCustomer(@PathVariable Long customerId) {
        log.info("Fetching loads for customer ID: {}", customerId);
        return ResponseEntity.ok(loadService.getLoadsByCustomer(customerId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<LoadResponseDTO>> getLoadsByStatus(@PathVariable String status) {
        log.info("Fetching loads with status: {}", status);
        return ResponseEntity.ok(loadService.getLoadsByStatus(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoad(@PathVariable Long id) {
        log.info("Deleting load with ID: {}", id);
        loadService.deleteLoad(id);
        return ResponseEntity.noContent().build();
    }
}
