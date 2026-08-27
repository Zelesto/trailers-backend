package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.CreateIncidentRequest;
import com.pgsa.trailers.dto.IncidentDTO;
import com.pgsa.trailers.dto.IncidentStatsDTO;
import com.pgsa.trailers.dto.UpdateIncidentRequest;
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
import java.util.ArrayList;
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

    /**
     * Create a new incident for a trip
     */
    public IncidentDTO createIncident(Long tripId, CreateIncidentRequest request) {
        log.info("Creating incident for tripId: {}", tripId);
        
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", tripId));
        
        // Validate trip status
        if (!tripService.canReportIncident(trip)) {
            throw new IllegalStateException("Cannot report incident for trip in status: " + trip.getStatus());
        }

        // Validate severity
        List<String> validSeverities = List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
        if (request.getSeverity() != null && !validSeverities.contains(request.getSeverity())) {
            throw new IllegalArgumentException("Invalid severity. Must be one of: " + validSeverities);
        }

        Incident incident = new Incident();
        incident.setTrip(trip);
        incident.setIncidentType(request.getIncidentType());
        incident.setSeverity(request.getSeverity() != null ? request.getSeverity() : "MEDIUM");
        incident.setDescription(request.getDescription());
        incident.setLocation(request.getLocation());
        incident.setRequiresAssistance(request.getRequiresAssistance() != null ? request.getRequiresAssistance() : false);
        incident.setResolved(false);
        incident.setReportedAt(LocalDateTime.now());
        incident.setAmount(request.getAmount());
        incident.setPaymentMethod(request.getPaymentMethod());
        incident.setReferenceNumber(request.getReferenceNumber());
        incident.setVoucherType(request.getVoucherType());
        incident.setEventType(request.getEventType());
        incident.setDirection(request.getDirection());
        incident.setAdditionalNotes(request.getAdditionalNotes());
        
        Incident saved = incidentRepository.save(incident);
        log.info("✅ Created incident {} for trip {}", saved.getId(), tripId);
        
        return toDTO(saved);
    }

    /**
     * Get all incidents for a trip (non-paginated)
     */
    @Transactional(readOnly = true)
    public List<IncidentDTO> getIncidentsByTripId(Long tripId) {
        log.info("Getting all incidents for tripId: {}", tripId);
        
        try {
            // Check if trip exists
            if (!tripRepository.existsById(tripId)) {
                log.warn("Trip not found with id: {}", tripId);
                return new ArrayList<>();
            }
            
            List<Incident> incidents = incidentRepository.findByTripId(tripId);
            log.info("Found {} incidents for trip {}", incidents.size(), tripId);
            
            return incidents.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching incidents for trip {}: {}", tripId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get incidents for a trip with pagination
     */
    @Transactional(readOnly = true)
    public Page<IncidentDTO> getIncidentsByTripId(Long tripId, Pageable pageable) {
        log.info("Getting paginated incidents for tripId: {}", tripId);
        
        try {
            // Check if trip exists
            if (!tripRepository.existsById(tripId)) {
                log.warn("Trip not found with id: {}", tripId);
                return Page.empty(pageable);
            }
            
            Page<Incident> incidents = incidentRepository.findByTripId(tripId, pageable);
            log.info("Found {} incidents (page) for trip {}", incidents.getTotalElements(), tripId);
            
            return incidents.map(this::toDTO);
        } catch (Exception e) {
            log.error("Error fetching paginated incidents for trip {}: {}", tripId, e.getMessage(), e);
            return Page.empty(pageable);
        }
    }

    /**
     * Get incident by ID
     */
    @Transactional(readOnly = true)
    public IncidentDTO getIncidentById(Long incidentId) {
        log.info("Getting incident by id: {}", incidentId);
        
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", incidentId));
        
        return toDTO(incident);
    }

    /**
     * Update an incident
     */
    public IncidentDTO updateIncident(Long incidentId, UpdateIncidentRequest request) {
        log.info("Updating incident id: {}", incidentId);
        
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", incidentId));
        
        boolean wasResolved = incident.getResolved() != null && incident.getResolved();
        
        // Update fields
        if (request.getIncidentType() != null) {
            incident.setIncidentType(request.getIncidentType());
        }
        if (request.getSeverity() != null) {
            incident.setSeverity(request.getSeverity());
        }
        if (request.getDescription() != null) {
            incident.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            incident.setLocation(request.getLocation());
        }
        if (request.getRequiresAssistance() != null) {
            incident.setRequiresAssistance(request.getRequiresAssistance());
        }
        if (request.getAmount() != null) {
            incident.setAmount(request.getAmount());
        }
        if (request.getPaymentMethod() != null) {
            incident.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getReferenceNumber() != null) {
            incident.setReferenceNumber(request.getReferenceNumber());
        }
        if (request.getVoucherType() != null) {
            incident.setVoucherType(request.getVoucherType());
        }
        if (request.getEventType() != null) {
            incident.setEventType(request.getEventType());
        }
        if (request.getDirection() != null) {
            incident.setDirection(request.getDirection());
        }
        if (request.getAdditionalNotes() != null) {
            incident.setAdditionalNotes(request.getAdditionalNotes());
        }
        
        // Handle resolution
        if (request.getResolved() != null) {
            incident.setResolved(request.getResolved());
            if (request.getResolved() && !wasResolved) {
                incident.setResolvedAt(LocalDateTime.now());
                if (request.getResolutionNotes() != null) {
                    incident.setResolutionNotes(request.getResolutionNotes());
                }
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
        log.info("✅ Incident updated: {}", updated.getId());
        
        return toDTO(updated);
    }

    /**
     * Delete an incident
     */
    public void deleteIncident(Long incidentId) {
        log.info("Deleting incident id: {}", incidentId);
        
        if (!incidentRepository.existsById(incidentId)) {
            throw new ResourceNotFoundException("Incident", "id", incidentId);
        }
        
        incidentRepository.deleteById(incidentId);
        log.info("✅ Incident deleted: {}", incidentId);
    }

    /**
     * Get active (unresolved) incidents for a trip
     */
    @Transactional(readOnly = true)
    public List<IncidentDTO> getActiveIncidents(Long tripId) {
        log.info("Getting active incidents for tripId: {}", tripId);
        
        try {
            if (!tripRepository.existsById(tripId)) {
                log.warn("Trip not found with id: {}", tripId);
                return new ArrayList<>();
            }
            
            List<Incident> incidents = incidentRepository.findActiveIncidentsByTripId(tripId);
            return incidents.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching active incidents for trip {}: {}", tripId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get incidents by resolved status
     */
    @Transactional(readOnly = true)
    public List<IncidentDTO> getIncidentsByResolvedStatus(Boolean resolved) {
        log.info("Getting incidents with resolved status: {}", resolved);
        
        try {
            List<Incident> incidents = incidentRepository.findByResolved(resolved);
            return incidents.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching incidents by resolved status: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get urgent incidents (requiring assistance)
     */
    @Transactional(readOnly = true)
    public List<IncidentDTO> getUrgentIncidents() {
        log.info("Getting urgent incidents");
        
        try {
            List<Incident> incidents = incidentRepository.findUrgentIncidents();
            return incidents.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching urgent incidents: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get incidents by severity
     */
    @Transactional(readOnly = true)
    public List<IncidentDTO> getIncidentsBySeverity(String severity) {
        log.info("Getting incidents with severity: {}", severity);
        
        try {
            List<Incident> incidents = incidentRepository.findBySeverity(severity);
            return incidents.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error fetching incidents by severity: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get incident statistics for a trip
     */
    @Transactional(readOnly = true)
    public IncidentStatsDTO getIncidentStats(Long tripId) {
        log.info("Getting incident stats for tripId: {}", tripId);
        
        try {
            // Check if trip exists
            if (!tripRepository.existsById(tripId)) {
                log.warn("Trip not found with id: {}", tripId);
                return IncidentStatsDTO.builder()
                        .totalIncidents(0L)
                        .activeIncidents(0L)
                        .urgentIncidents(0L)
                        .build();
            }
            
            Long totalIncidents = incidentRepository.countByTripId(tripId);
            Long activeIncidents = incidentRepository.countByTripIdAndResolved(tripId, false);
            
            // Count urgent incidents
            long urgentCount = incidentRepository.findUrgentIncidents().stream()
                    .filter(i -> i.getTrip().getId().equals(tripId))
                    .count();
            
            return IncidentStatsDTO.builder()
                    .totalIncidents(totalIncidents != null ? totalIncidents : 0L)
                    .activeIncidents(activeIncidents != null ? activeIncidents : 0L)
                    .urgentIncidents(urgentCount)
                    .build();
        } catch (Exception e) {
            log.error("Error fetching incident stats for trip {}: {}", tripId, e.getMessage(), e);
            return IncidentStatsDTO.builder()
                    .totalIncidents(0L)
                    .activeIncidents(0L)
                    .urgentIncidents(0L)
                    .build();
        }
    }

    /**
     * Map Incident entity to DTO
     */
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
        dto.setAmount(incident.getAmount());
        dto.setPaymentMethod(incident.getPaymentMethod());
        dto.setReferenceNumber(incident.getReferenceNumber());
        dto.setVoucherType(incident.getVoucherType());
        dto.setEventType(incident.getEventType());
        dto.setDirection(incident.getDirection());
        dto.setAdditionalNotes(incident.getAdditionalNotes());
        return dto;
    }
}
