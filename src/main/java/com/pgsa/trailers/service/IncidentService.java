package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.*;
import com.pgsa.trailers.entity.ops.Incident;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.entity.ResourceNotFoundException;
import com.pgsa.trailers.repository.IncidentRepository;
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
public class IncidentService {
    private final IncidentRepository incidentRepository;
    private final TripRepository tripRepository;
    private final TripService tripService;

    public IncidentDTO createIncident(Long tripId, CreateIncidentRequest request) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));
        
        // Validate trip status
        if (!tripService.canReportIncident(trip)) {
            throw new IllegalStateException("Cannot report incident for trip in status: " + trip.getStatus());
        }

        // Validate severity
        List<String> validSeverities = List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
        if (!validSeverities.contains(request.getSeverity())) {
            throw new IllegalArgumentException("Invalid severity. Must be one of: " + validSeverities);
        }

        Incident incident = new Incident();
        incident.setTrip(trip);
        incident.setIncidentType(request.getIncidentType());
        incident.setSeverity(request.getSeverity());
        incident.setDescription(request.getDescription());
        incident.setLocation(request.getLocation());
        incident.setRequiresAssistance(request.getRequiresAssistance() != null ? request.getRequiresAssistance() : false);
        incident.setResolved(false);
        
        Incident saved = incidentRepository.save(incident);
        log.info("Created incident {} for trip {}", saved.getId(), tripId);
        
        return toDTO(saved);
    }

    public IncidentDTO updateIncident(Long incidentId, UpdateIncidentRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", incidentId));
        
        boolean wasResolved = incident.getResolved();
        
        if (request.getResolved() != null) {
            incident.setResolved(request.getResolved());
            if (request.getResolved() && !wasResolved) {
                incident.setResolvedAt(LocalDateTime.now());
                incident.setResolutionNotes(request.getResolutionNotes());
                log.info("Resolved incident {}", incidentId);
            } else if (!request.getResolved() && wasResolved) {
                incident.setResolvedAt(null);
                incident.setResolutionNotes(null);
            }
        }
        
        if (request.getResolutionNotes() != null && !request.getResolutionNotes().isEmpty()) {
            incident.setResolutionNotes(request.getResolutionNotes());
        }
        
        Incident updated = incidentRepository.save(incident);
        return toDTO(updated);
    }

    public List<IncidentDTO> getIncidentsByTripId(Long tripId) {
        List<Incident> incidents = incidentRepository.findByTripId(tripId);
        return incidents.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

   public Page<IncidentDTO> getIncidentsByTripId(Long tripId, Pageable pageable) {
    return incidentRepository.findByTripId(tripId, pageable)  // Changed from findAllByTripId to findByTripId
        .map(this::toDTO);
}
    
    public IncidentDTO getIncidentById(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", incidentId));
        return toDTO(incident);
    }

    public void deleteIncident(Long incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResourceNotFoundException("Incident", "id", incidentId);
        }
        incidentRepository.deleteById(incidentId);
        log.info("Deleted incident {}", incidentId);
    }

    public List<IncidentDTO> getActiveIncidents(Long tripId) {
        List<Incident> incidents = incidentRepository.findActiveIncidentsByTripId(tripId);
        return incidents.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<IncidentDTO> getIncidentsByResolvedStatus(Boolean resolved) {
        List<Incident> incidents = incidentRepository.findByResolved(resolved);
        return incidents.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<IncidentDTO> getUrgentIncidents() {
        List<Incident> incidents = incidentRepository.findUrgentIncidents();
        return incidents.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<IncidentDTO> getIncidentsBySeverity(String severity) {
        List<Incident> incidents = incidentRepository.findBySeverity(severity);
        return incidents.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public IncidentStatsDTO getIncidentStats(Long tripId) {
        Long totalIncidents = incidentRepository.countByTripId(tripId);
        Long activeIncidents = (long) incidentRepository.findActiveIncidentsByTripId(tripId).size();
        Long urgentIncidents = (long) incidentRepository.findUrgentIncidents().stream()
            .filter(i -> i.getTrip().getId().equals(tripId))
            .count();
        
        return IncidentStatsDTO.builder()
            .totalIncidents(totalIncidents)
            .activeIncidents(activeIncidents)
            .urgentIncidents(urgentIncidents)
            .build();
    }

    private IncidentDTO toDTO(Incident incident) {
        IncidentDTO dto = new IncidentDTO();
        dto.setId(incident.getId());
        dto.setTripId(incident.getTrip().getId());
        dto.setTripNumber(incident.getTrip().getTripNumber());
        dto.setIncidentType(incident.getIncidentType());
        dto.setSeverity(incident.getSeverity());
        dto.setDescription(incident.getDescription());
        dto.setLocation(incident.getLocation());
        dto.setRequiresAssistance(incident.getRequiresAssistance());
        dto.setResolved(incident.getResolved());
        dto.setResolutionNotes(incident.getResolutionNotes());
        dto.setReportedAt(incident.getReportedAt());
        dto.setResolvedAt(incident.getResolvedAt());
        dto.setCreatedAt(incident.getCreatedAt());
        dto.setUpdatedAt(incident.getUpdatedAt());
        return dto;
    }
}
