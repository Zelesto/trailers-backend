package com.pgsa.trailers.controller.ops;

import com.pgsa.trailers.dto.CreateIncidentRequest;  // ✅
import com.pgsa.trailers.dto.IncidentDTO;  // ✅ Fixed typo
import com.pgsa.trailers.dto.UpdateIncidentRequest;  // ✅
import com.pgsa.trailers.dto.IncidentStatsDTO;  // ✅
import com.pgsa.trailers.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trips/{tripId}/incidents")
@RequiredArgsConstructor
@Tag(name = "Incident Management", description = "API for managing trip incidents")
@Slf4j
public class IncidentController {
    private final IncidentService incidentService;

    @PostMapping
    @Operation(summary = "Report a new incident for a trip")
    public ResponseEntity<IncidentDTO> createIncident(
            @PathVariable Long tripId,
            @Valid @RequestBody CreateIncidentRequest request) {
        log.info("Creating incident for tripId: {}", tripId);
        IncidentDTO incident = incidentService.createIncident(tripId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(incident);
    }

    @GetMapping
    @Operation(summary = "Get all incidents for a trip with pagination")
    public ResponseEntity<Page<IncidentDTO>> getTripIncidents(
            @PathVariable Long tripId,
            @PageableDefault(sort = "reportedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Getting paginated incidents for tripId: {}", tripId);
        Page<IncidentDTO> incidents = incidentService.getIncidentsByTripId(tripId, pageable);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/list")
    @Operation(summary = "Get all incidents for a trip (non-paginated)")
    public ResponseEntity<List<IncidentDTO>> getTripIncidentsList(@PathVariable Long tripId) {
        log.info("Getting all incidents for tripId: {}", tripId);
        List<IncidentDTO> incidents = incidentService.getIncidentsByTripId(tripId);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/{incidentId}")
    @Operation(summary = "Get incident by ID")
    public ResponseEntity<IncidentDTO> getIncident(
            @PathVariable Long tripId,
            @PathVariable Long incidentId) {
        log.info("Getting incident {} for tripId: {}", incidentId, tripId);
        IncidentDTO incident = incidentService.getIncidentById(incidentId);
        return ResponseEntity.ok(incident);
    }

    @PutMapping("/{incidentId}")
    @Operation(summary = "Update an incident")
    public ResponseEntity<IncidentDTO> updateIncident(
            @PathVariable Long tripId,
            @PathVariable Long incidentId,
            @Valid @RequestBody UpdateIncidentRequest request) {
        log.info("Updating incident {} for tripId: {}", incidentId, tripId);
        IncidentDTO incident = incidentService.updateIncident(incidentId, request);
        return ResponseEntity.ok(incident);
    }

    @DeleteMapping("/{incidentId}")
    @Operation(summary = "Delete an incident")
    public ResponseEntity<Void> deleteIncident(
            @PathVariable Long tripId,
            @PathVariable Long incidentId) {
        log.info("Deleting incident {} for tripId: {}", incidentId, tripId);
        incidentService.deleteIncident(incidentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    @Operation(summary = "Get active (unresolved) incidents for a trip")
    public ResponseEntity<List<IncidentDTO>> getActiveIncidents(@PathVariable Long tripId) {
        log.info("Getting active incidents for tripId: {}", tripId);
        List<IncidentDTO> incidents = incidentService.getActiveIncidents(tripId);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/resolved/{resolved}")
    @Operation(summary = "Get incidents by resolved status")
    public ResponseEntity<List<IncidentDTO>> getIncidentsByResolvedStatus(
            @PathVariable Long tripId,
            @PathVariable Boolean resolved) {
        log.info("Getting incidents with resolved status {} for tripId: {}", resolved, tripId);
        List<IncidentDTO> incidents = incidentService.getIncidentsByResolvedStatus(resolved);
        // Filter to only include incidents for this trip
        List<IncidentDTO> tripIncidents = incidents.stream()
            .filter(i -> tripId.equals(i.getTripId()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(tripIncidents);
    }

    @GetMapping("/severity/{severity}")
    @Operation(summary = "Get incidents by severity level")
    public ResponseEntity<List<IncidentDTO>> getIncidentsBySeverity(
            @PathVariable Long tripId,
            @PathVariable String severity) {
        log.info("Getting incidents with severity {} for tripId: {}", severity, tripId);
        List<IncidentDTO> incidents = incidentService.getIncidentsBySeverity(severity);
        // Filter to only include incidents for this trip
        List<IncidentDTO> tripIncidents = incidents.stream()
            .filter(i -> tripId.equals(i.getTripId()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(tripIncidents);
    }

    @GetMapping("/urgent")
    @Operation(summary = "Get urgent incidents requiring assistance")
    public ResponseEntity<List<IncidentDTO>> getUrgentIncidents(@PathVariable Long tripId) {
        log.info("Getting urgent incidents for tripId: {}", tripId);
        List<IncidentDTO> incidents = incidentService.getUrgentIncidents();
        List<IncidentDTO> tripUrgentIncidents = incidents.stream()
            .filter(i -> tripId.equals(i.getTripId()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(tripUrgentIncidents);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get incident statistics for a trip")
    public ResponseEntity<IncidentStatsDTO> getIncidentStats(@PathVariable Long tripId) {
        log.info("Getting incident stats for tripId: {}", tripId);
        IncidentStatsDTO stats = incidentService.getIncidentStats(tripId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/search")
    @Operation(summary = "Search incidents with filters")
    public ResponseEntity<List<IncidentDTO>> searchIncidents(
            @PathVariable Long tripId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) Boolean requiresAssistance,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        
        log.info("Searching incidents for tripId: {} with filters: severity={}, resolved={}, requiresAssistance={}, fromDate={}, toDate={}",
                tripId, severity, resolved, requiresAssistance, fromDate, toDate);
        
        List<IncidentDTO> allIncidents = incidentService.getIncidentsByTripId(tripId);
        
        List<IncidentDTO> filtered = allIncidents.stream()
            .filter(i -> severity == null || severity.equalsIgnoreCase(i.getSeverity()))
            .filter(i -> resolved == null || resolved.equals(i.getResolved()))
            .filter(i -> requiresAssistance == null || requiresAssistance.equals(i.getRequiresAssistance()))
            .filter(i -> fromDate == null || !i.getReportedAt().toLocalDate().isBefore(fromDate))
            .filter(i -> toDate == null || !i.getReportedAt().toLocalDate().isAfter(toDate))
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(filtered);
    }
}
