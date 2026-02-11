package com.pgsa.trailers.controller.ops;

import com.pgsa.trailers.dto.CreateIncidentRequest;
import com.pgsa.trailers.dto.IncidentDTO;
import com.pgsa.trailers.dto.UpdateIncidentRequest;
import com.pgsa.trailers.dto.IncidentStatsDTO;
import com.pgsa.trailers.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}/incidents")
@RequiredArgsConstructor
@Tag(name = "Incident Management", description = "API for managing trip incidents")
public class IncidentController {
    private final IncidentService incidentService;

    @PostMapping
    @Operation(summary = "Report a new incident for a trip")
    public ResponseEntity<IncidentDTO> createIncident(
            @PathVariable Long tripId,
            @Valid @RequestBody CreateIncidentRequest request) {
        IncidentDTO incident = incidentService.createIncident(tripId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(incident);
    }

    @GetMapping
    @Operation(summary = "Get all incidents for a trip with pagination")
    public ResponseEntity<Page<IncidentDTO>> getTripIncidents(
            @PathVariable Long tripId,
            @PageableDefault(sort = "reportedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<IncidentDTO> incidents = incidentService.getIncidentsByTripId(tripId, pageable);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/list")
    @Operation(summary = "Get all incidents for a trip (non-paginated)")
    public ResponseEntity<List<IncidentDTO>> getTripIncidentsList(@PathVariable Long tripId) {
        List<IncidentDTO> incidents = incidentService.getIncidentsByTripId(tripId);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/{incidentId}")
    @Operation(summary = "Get incident by ID")
    public ResponseEntity<IncidentDTO> getIncident(
            @PathVariable Long tripId,
            @PathVariable Long incidentId) {
        IncidentDTO incident = incidentService.getIncidentById(incidentId);
        return ResponseEntity.ok(incident);
    }

    @PutMapping("/{incidentId}")
    @Operation(summary = "Update an incident")
    public ResponseEntity<IncidentDTO> updateIncident(
            @PathVariable Long tripId,
            @PathVariable Long incidentId,
            @Valid @RequestBody UpdateIncidentRequest request) {
        IncidentDTO incident = incidentService.updateIncident(incidentId, request);
        return ResponseEntity.ok(incident);
    }

    @DeleteMapping("/{incidentId}")
    @Operation(summary = "Delete an incident")
    public ResponseEntity<Void> deleteIncident(
            @PathVariable Long tripId,
            @PathVariable Long incidentId) {
        incidentService.deleteIncident(incidentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    @Operation(summary = "Get active (unresolved) incidents for a trip")
    public ResponseEntity<List<IncidentDTO>> getActiveIncidents(@PathVariable Long tripId) {
        List<IncidentDTO> incidents = incidentService.getActiveIncidents(tripId);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/resolved/{resolved}")
    @Operation(summary = "Get incidents by resolved status")
    public ResponseEntity<List<IncidentDTO>> getIncidentsByResolvedStatus(
            @PathVariable Long tripId,
            @PathVariable Boolean resolved) {
        List<IncidentDTO> incidents = incidentService.getIncidentsByResolvedStatus(resolved);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/severity/{severity}")
    @Operation(summary = "Get incidents by severity level")
    public ResponseEntity<List<IncidentDTO>> getIncidentsBySeverity(
            @PathVariable Long tripId,
            @PathVariable String severity) {
        List<IncidentDTO> incidents = incidentService.getIncidentsBySeverity(severity);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/urgent")
    @Operation(summary = "Get urgent incidents requiring assistance")
    public ResponseEntity<List<IncidentDTO>> getUrgentIncidents(@PathVariable Long tripId) {
        List<IncidentDTO> incidents = incidentService.getUrgentIncidents();
        List<IncidentDTO> tripUrgentIncidents = incidents.stream()
            .filter(i -> i.getTripId().equals(tripId))
            .collect(Collectors.toList());
        return ResponseEntity.ok(tripUrgentIncidents);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get incident statistics for a trip")
    public ResponseEntity<IncidentStatsDTO> getIncidentStats(@PathVariable Long tripId) {
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
        
        // Implementation would filter incidents based on parameters
        List<IncidentDTO> allIncidents = incidentService.getIncidentsByTripId(tripId);
        
        List<IncidentDTO> filtered = allIncidents.stream()
            .filter(i -> severity == null || i.getSeverity().equalsIgnoreCase(severity))
            .filter(i -> resolved == null || i.getResolved().equals(resolved))
            .filter(i -> requiresAssistance == null || i.getRequiresAssistance().equals(requiresAssistance))
            .filter(i -> fromDate == null || !i.getReportedAt().toLocalDate().isBefore(fromDate))
            .filter(i -> toDate == null || !i.getReportedAt().toLocalDate().isAfter(toDate))
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(filtered);
    }
}
