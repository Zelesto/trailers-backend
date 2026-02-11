package com.pgsa.trailers.service.ops;

import com.pgsa.trailers.dto.ops.CreateIncidentRequest;
import com.pgsa.trailers.dto.ops.IncidentDTO;
import com.pgsa.trailers.dto.ops.UpdateIncidentRequest;
import com.pgsa.trailers.entity.ops.Incident;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.exception.ResourceNotFoundException;
import com.pgsa.trailers.repository.ops.IncidentRepository;
import com.pgsa.trailers.repository.ops.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class IncidentService {
    private final IncidentRepository incidentRepository;
    private final TripRepository tripRepository;
    private final TripService tripService;

    public IncidentDTO createIncident(Long tripId, CreateIncidentRequest request) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));
        
        // Validate that trip can have incidents (should be active/in progress)
        if (!tripService.canReportIncident(trip)) {
            throw new IllegalStateException("Cannot report incident for trip in status: " + trip.getStatus());
        }

        Incident incident = new Incident();
        incident.setTrip(trip);
        incident.setIncidentType(request.getIncidentType());
        incident.setSeverity(request.getSeverity() != null ? request.getSeverity() : "MEDIUM");
        incident.setDescription(request.getDescription());
        incident.setLocation(request.getLocation());
        incident.setRequiresAssistance(request.getRequiresAssistance() != null ? request.getRequiresAssistance() : false);
        incident.setResolved(false);
        
        Incident saved = incidentRepository.save(incident);
        return toDTO(saved);
    }

    public IncidentDTO updateIncident(Long incidentId, UpdateIncidentRequest request) {
        Incident incident = incidentRepository.findById(incidentId)
            .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", incidentId));
        
        if (request.getResolved() != null) {
            incident.setResolved(request.getResolved());
            if (request.getResolved()) {
                incident.setResolvedAt(LocalDateTime.now());
                incident.setResolutionNotes(request.getResolutionNotes());
            } else {
                incident.setResolvedAt(null);
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
        return dto;
    }
}
