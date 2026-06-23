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
