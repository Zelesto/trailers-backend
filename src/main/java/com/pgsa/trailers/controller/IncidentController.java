package com.pgsa.trailers.controller.ops;

import com.pgsa.trailers.dto.ops.CreateIncidentRequest;
import com.pgsa.trailers.dto.ops.IncidentDTO;
import com.pgsa.trailers.dto.ops.UpdateIncidentRequest;
import com.pgsa.trailers.service.ops.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    @Operation(summary = "Get all incidents for a trip")
    public ResponseEntity<List<IncidentDTO>> getTripIncidents(@PathVariable Long tripId) {
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
}
