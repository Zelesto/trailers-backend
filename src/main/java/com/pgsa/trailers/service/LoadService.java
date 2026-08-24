// src/main/java/com/pgsa/trailers/service/LoadService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.LoadRequestDTO;
import com.pgsa.trailers.dto.LoadResponseDTO;
import com.pgsa.trailers.dto.TripSummaryDTO;
import com.pgsa.trailers.entity.ops.Customer;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.CustomerRepository;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoadService {

    // ============================================================
    // CONSTANTS FOR STATUS VALUES (from enum_master table)
    // ============================================================
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PLANNED = "PLANNED";
    public static final String STATUS_IN_TRANSIT = "IN_TRANSIT";
    public static final String STATUS_LOADING = "LOADING";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String TRIP_STATUS_PLANNED = "PLANNED";
    public static final String TRIP_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String TRIP_STATUS_COMPLETED = "COMPLETED";
    public static final String TRIP_STATUS_FINALIZED = "FINALIZED";

    // ============================================================
    // DEPENDENCIES - DECLARED ONCE
    // ============================================================
    private final LoadRepository loadRepository;
    private final TripRepository tripRepository;
    private final CustomerRepository customerRepository;
    private final SequenceService sequenceService;
    private final JdbcTemplate jdbcTemplate;

    // =============================================
    // UPDATE LOAD DISTANCES
    // =============================================

    @Transactional
    public void updateLoadDistances(String loadId) {
        log.info("📦 Updating distances for Load ID: {}", loadId);
    
        try {
            Load load = loadRepository.findByLoadNumber(loadId)
                    .orElseThrow(() -> new RuntimeException("Load not found: " + loadId));
    
            List<Trip> trips = tripRepository.findByLoadId(loadId);
            
            log.info("📊 Found {} trips for Load {}", trips.size(), loadId);
    
            if (trips.isEmpty()) {
                log.warn("⚠️ No trips found for Load {}", loadId);
                load.setTotalCalculatedDistanceKm(BigDecimal.ZERO);
                load.setTotalActualDistanceKm(BigDecimal.ZERO);
                load.setTotalCalculatedDistance(BigDecimal.ZERO);
                load.setTotalActualDistance(BigDecimal.ZERO);
                load.setDistanceCalculated(false);
                load.setDistanceCalculatedAt(LocalDateTime.now());
                loadRepository.save(load);
                log.info("✅ Load {} reset to zero distances", loadId);
                return;
            }
    
            // Calculate totals
            BigDecimal totalCalculated = trips.stream()
                    .map(Trip::getCalculatedDistanceKm)
                    .filter(d -> d != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
    
            BigDecimal totalActual = trips.stream()
                    .map(Trip::getActualDistanceKm)
                    .filter(d -> d != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
    
            log.info("📊 Calculated totals for Load {}: calculated={} km, actual={} km", 
                loadId, totalCalculated, totalActual);
    
            // Update load
            load.setTotalCalculatedDistanceKm(totalCalculated);
            load.setTotalActualDistanceKm(totalActual);
            load.setTotalCalculatedDistance(totalCalculated);
            load.setTotalActualDistance(totalActual);
            load.setTotalEstimatedDistance(totalCalculated);
            load.setDistanceCalculated(totalCalculated.compareTo(BigDecimal.ZERO) > 0);
            load.setDistanceCalculatedAt(LocalDateTime.now());
    
            Load saved = loadRepository.save(load);
            log.info("✅ Load {} distances updated. Calculated: {} km, Actual: {} km",
                    loadId, totalCalculated, totalActual);
    
        } catch (Exception e) {
            log.error("❌ Error updating load distances for {}: {}", loadId, e.getMessage(), e);
        }
    }

    @Transactional
    public void updateAllLoadDistances() {
        log.info("📦 Updating all load distances...");
        List<Load> loads = loadRepository.findAll();
        for (Load load : loads) {
            updateLoadDistances(load.getLoadNumber());
        }
        log.info("✅ All load distances updated");
    }

    // =============================================
    // GENERATE REFERENCE NUMBER
    // =============================================

    private String generateReferenceNumber() {
        try {
            String year = String.valueOf(java.time.Year.now().getValue());
            String prefix = "REF-" + year + "-";
            
            Long nextNumber = jdbcTemplate.queryForObject(
                "INSERT INTO sequence (table_name, year, next_number, created_at, updated_at) " +
                "VALUES (?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                "ON CONFLICT (table_name, year) DO UPDATE SET next_number = sequence.next_number + 1 " +
                "RETURNING next_number - 1",
                new Object[]{"loadref", year},
                Long.class
            );
            
            String referenceNumber = prefix + String.format("%03d", nextNumber);
            log.info("✅ Generated load reference number: {}", referenceNumber);
            return referenceNumber;
            
        } catch (Exception e) {
            log.error("❌ Error generating load reference number: {}", e.getMessage());
            return "REF-" + System.currentTimeMillis();
        }
    }

    // =============================================
    // CREATE
    // =============================================

    public LoadResponseDTO createLoad(LoadRequestDTO request, Long userId) {
        log.info("Creating load for customer: {}, date: {}", request.getCustomerId(), request.getLoadingDate());

        if (request.getCustomerId() != null && request.getLoadingDate() != null) {
            Load existingLoad = findMergeCandidate(request.getCustomerId(), request.getLoadingDate());
            if (existingLoad != null) {
                log.info("Found existing load {} that could be merged", existingLoad.getLoadNumber());
                LoadResponseDTO response = mapToResponseDTO(existingLoad);
                response.setMergeSuggestion(true);
                response.setMergeMessage("A load already exists for this customer on " + 
                    request.getLoadingDate().toLocalDate() + 
                    ". Would you like to add this trip to the existing load?");
                return response;
            }
        }

        if (request.getCustomerId() != null && !customerRepository.existsById(request.getCustomerId())) {
            throw new RuntimeException("Customer not found with ID: " + request.getCustomerId());
        }

        String loadNumber = sequenceService.generateFormattedSequence("load", "LOAD");
        String referenceNumber = generateReferenceNumber();

        Load load = Load.builder()
                .loadNumber(loadNumber)
                .referenceNumber(referenceNumber)
                .description(request.getDescription())
                .customerId(request.getCustomerId())
                .weightKg(request.getWeightKg())
                .volumeCubicM(request.getVolumeCubicM())
                .loadingDate(request.getLoadingDate())
                .unloadingDate(request.getUnloadingDate())
                .status(STATUS_PENDING)
                .commodityType(request.getCommodityType())
                .palletCount(request.getPalletCount())
                .containerNumber(request.getContainerNumber())
                .hazardousMaterial(request.getHazardousMaterial())
                .specialHandling(request.getSpecialHandling())
                .estimatedValue(request.getEstimatedValue())
                .actualValue(request.getActualValue())
                .priority(request.getPriority() != null ? request.getPriority() : "NORMAL")
                .originLocation(request.getOriginLocation())
                .destinationLocation(request.getDestinationLocation())
                .handlingInstructions(request.getHandlingInstructions())
                .packagingType(request.getPackagingType())
                .hazardClass(request.getHazardClass())
                .temperatureRequirements(request.getTemperatureRequirements())
                .insurancePolicyNumber(request.getInsurancePolicyNumber())
                .insuranceExpiry(request.getInsuranceExpiry())
                .customsClearanceStatus(request.getCustomsClearanceStatus())
                .warehouseId(request.getWarehouseId())
                .supervisorId(request.getSupervisorId())
                .build();

        load.setCreatedBy(String.valueOf(userId));
        load.setLastStatusUpdate(LocalDateTime.now());

        Load saved = loadRepository.save(load);
        log.info("Created load with ID: {}, Number: {}, Reference: {}", 
            saved.getId(), saved.getLoadNumber(), saved.getReferenceNumber());

        if (request.getTripIds() != null && !request.getTripIds().isEmpty()) {
            addTripsToLoad(saved.getLoadNumber(), request.getTripIds(), userId);
        }

        return mapToResponseDTO(saved);
    }

    // =============================================
    // READ
    // =============================================

    @Transactional(readOnly = true)
    public LoadResponseDTO getLoadById(Long id) {
        Load load = loadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Load not found with ID: " + id));
        return mapToResponseDTO(load);
    }

    @Transactional(readOnly = true)
    public LoadResponseDTO getLoadByNumber(String loadNumber) {
        Load load = loadRepository.findByLoadNumber(loadNumber)
                .orElseThrow(() -> new RuntimeException("Load not found with number: " + loadNumber));
        return mapToResponseDTO(load);
    }

    @Transactional(readOnly = true)
    public LoadResponseDTO getLoadByReferenceNumber(String referenceNumber) {
        Load load = loadRepository.findByReferenceNumber(referenceNumber)
                .orElseThrow(() -> new RuntimeException("Load not found with reference: " + referenceNumber));
        return mapToResponseDTO(load);
    }

    @Transactional(readOnly = true)
    public Page<LoadResponseDTO> getAllLoads(Pageable pageable) {
        log.info("Fetching all loads with pagination: page={}, size={}", 
            pageable.getPageNumber(), pageable.getPageSize());
        
        return loadRepository.findAll(pageable)
                .map(this::mapToResponseDTO);
    }

    @Transactional(readOnly = true)
    public Page<LoadResponseDTO> searchLoads(String search, Pageable pageable) {
        log.info("Searching loads with term: {}", search);
        if (search == null || search.trim().isEmpty()) {
            return getAllLoads(pageable);
        }
        return loadRepository.searchLoads(search.trim(), pageable)
                .map(this::mapToResponseDTO);
    }

    @Transactional(readOnly = true)
    public List<LoadResponseDTO> getLoadsByCustomer(Long customerId) {
        return loadRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LoadResponseDTO> getLoadsByStatus(String status) {
        return loadRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    // =============================================
    // UPDATE
    // =============================================

    @Transactional
    public LoadResponseDTO updateLoad(Long id, LoadRequestDTO request, Long userId) {
        log.info("Updating load with ID: {}", id);
        
        Load load = loadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Load not found with ID: " + id));
        
        load.setDescription(request.getDescription());
        load.setCustomerId(request.getCustomerId());
        load.setWeightKg(request.getWeightKg());
        load.setVolumeCubicM(request.getVolumeCubicM());
        load.setLoadingDate(request.getLoadingDate());
        load.setUnloadingDate(request.getUnloadingDate());
        
        if (request.getStatus() != null) {
            load.setStatus(request.getStatus().toUpperCase());
        }
        
        load.setCommodityType(request.getCommodityType());
        load.setPalletCount(request.getPalletCount());
        load.setContainerNumber(request.getContainerNumber());
        load.setHazardousMaterial(request.getHazardousMaterial());
        load.setSpecialHandling(request.getSpecialHandling());
        load.setEstimatedValue(request.getEstimatedValue());
        load.setActualValue(request.getActualValue());
        load.setPriority(request.getPriority());
        
        load.setOriginLocation(request.getOriginLocation());
        load.setDestinationLocation(request.getDestinationLocation());
        load.setHandlingInstructions(request.getHandlingInstructions());
        load.setPackagingType(request.getPackagingType());
        load.setHazardClass(request.getHazardClass());
        load.setTemperatureRequirements(request.getTemperatureRequirements());

        load.setInsurancePolicyNumber(request.getInsurancePolicyNumber());
        load.setInsuranceExpiry(request.getInsuranceExpiry());
        load.setCustomsClearanceStatus(request.getCustomsClearanceStatus());
        load.setWarehouseId(request.getWarehouseId());
        load.setSupervisorId(request.getSupervisorId());
        
        load.setUpdatedAt(LocalDateTime.now());
        load.setUpdatedBy(String.valueOf(userId));
        load.setLastStatusUpdate(LocalDateTime.now());
        
        Load updated = loadRepository.save(load);
        log.info("Updated load with ID: {}", updated.getId());
        return mapToResponseDTO(updated);
    }

    @Transactional
    public LoadResponseDTO updateLoadStatus(Long id, String status, Long userId) {
        log.info("Updating load {} status to: {}", id, status);
        
        Load load = loadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Load not found with ID: " + id));
        
        String newStatus = status.toUpperCase();
        load.setStatus(newStatus);
        load.setLastStatusUpdate(LocalDateTime.now());
        load.setUpdatedBy(String.valueOf(userId));
        
        Load updated = loadRepository.save(load);
        log.info("Updated load {} status to: {}", id, status);
        
        return mapToResponseDTO(updated);
    }

    @Transactional
    public LoadResponseDTO addTripsToLoad(String loadNumber, List<Long> tripIds, Long userId) {
        log.info("Adding {} trips to load {}", tripIds.size(), loadNumber);
        
        Load load = loadRepository.findByLoadNumber(loadNumber)
                .orElseThrow(() -> new RuntimeException("Load not found with number: " + loadNumber));

        if (tripIds == null || tripIds.isEmpty()) {
            log.warn("No trip IDs provided to add to load {}", loadNumber);
            return mapToResponseDTO(load);
        }

        List<Trip> trips = tripRepository.findAllById(tripIds);
        
        if (load.getCustomerId() != null) {
            for (Trip trip : trips) {
                if (trip.getCustomerId() != null && !trip.getCustomerId().equals(load.getCustomerId())) {
                    throw new RuntimeException("Trip " + trip.getTripNumber() + 
                        " belongs to a different customer. Cannot add to this load.");
                }
            }
        }

        for (Trip trip : trips) {
            load.addTrip(trip);
            trip.setUpdatedAt(LocalDateTime.now());
            trip.setUpdatedBy(userId);
            tripRepository.save(trip);
        }

        updateLoadFromTrips(load);

        Load updated = loadRepository.save(load);
        log.info("Added {} trips to load {}", trips.size(), loadNumber);

        return mapToResponseDTO(updated);
    }

    // =============================================
    // DELETE
    // =============================================

    public void deleteLoad(Long id) {
        Load load = loadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Load not found with ID: " + id));
        
        if (load.getTrips() != null && !load.getTrips().isEmpty()) {
            throw new RuntimeException("Cannot delete load with trips. Remove all trips first.");
        }
        
        loadRepository.deleteById(id);
        log.info("Deleted load with ID: {}", id);
    }

    // =============================================
    // SMART MERGE
    // =============================================

    @Transactional(readOnly = true)
    public Load findMergeCandidate(Long customerId, LocalDateTime loadingDate) {
        if (customerId == null || loadingDate == null) {
            return null;
        }
        
        LocalDate date = loadingDate.toLocalDate();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<Load> loads = loadRepository.findByCustomerIdAndLoadingDateBetween(
            customerId, startOfDay, endOfDay);

        return loads.stream()
                .filter(l -> !STATUS_COMPLETED.equals(l.getStatus()) && !STATUS_CANCELLED.equals(l.getStatus()))
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Trip> findMergeableTrips(Long customerId, LocalDateTime plannedDate) {
        if (customerId == null || plannedDate == null) {
            return new ArrayList<>();
        }
        
        LocalDate date = plannedDate.toLocalDate();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<Trip> allTrips = tripRepository.findByCustomerId(customerId, Pageable.unpaged())
                .getContent();
        
        return allTrips.stream()
                .filter(t -> t.getLoadId() == null || t.getLoadId().isEmpty())
                .filter(t -> t.getPlannedStartDate() != null)
                .filter(t -> !t.getPlannedStartDate().isBefore(startOfDay) && 
                           !t.getPlannedStartDate().isAfter(endOfDay))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TripSummaryDTO> findMergeableTripsDTO(Long customerId, LocalDateTime plannedDate) {
        List<Trip> trips = findMergeableTrips(customerId, plannedDate);
        List<TripSummaryDTO> result = new ArrayList<>();
        for (Trip trip : trips) {
            result.add(createTripSummaryDTO(trip));
        }
        return result;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    public LoadResponseDTO smartMergeTrips(Long customerId, LocalDateTime plannedDate, Long userId) {
        log.info("Smart merging trips for customer {} on {}", customerId, plannedDate);

        List<Trip> mergeableTrips = findMergeableTrips(customerId, plannedDate);
        
        if (mergeableTrips.isEmpty()) {
            throw new RuntimeException("No mergeable trips found for this customer on this date");
        }

        Load existingLoad = findMergeCandidate(customerId, plannedDate);
        
        if (existingLoad != null) {
            List<Long> tripIds = mergeableTrips.stream()
                .map(Trip::getId)
                .collect(Collectors.toList());
            return addTripsToLoad(existingLoad.getLoadNumber(), tripIds, userId);
        }

        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new RuntimeException("Customer not found"));

        LoadRequestDTO loadRequest = LoadRequestDTO.builder()
            .customerId(customerId)
            .loadingDate(plannedDate)
            .description("Auto-merged load for " + customer.getName() + 
                " - " + plannedDate.toLocalDate())
            .tripIds(mergeableTrips.stream()
                .map(Trip::getId)
                .collect(Collectors.toList()))
            .build();

        if (!mergeableTrips.isEmpty()) {
            Trip firstTrip = mergeableTrips.get(0);
            loadRequest.setCommodityType(firstTrip.getCommodityType());
            loadRequest.setOriginLocation(firstTrip.getOriginLocation());
            loadRequest.setDestinationLocation(firstTrip.getDestinationLocation());
        }

        LoadResponseDTO response = createLoad(loadRequest, userId);
        response.setMergeMessage("Successfully merged " + mergeableTrips.size() + 
            " trips into load " + response.getLoadNumber());
        
        return response;
    }

    // =============================================
    // PRIVATE HELPERS
    // =============================================

    private void updateLoadFromTrips(Load load) {
        if (load.getTrips() == null || load.getTrips().isEmpty()) {
            return;
        }

        BigDecimal totalWeight = load.getTrips().stream()
                .map(Trip::getCargoWeight)
                .filter(w -> w != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        load.setWeightKg(totalWeight);

        BigDecimal totalValue = load.getTrips().stream()
                .map(Trip::getCargoValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        load.setActualValue(totalValue);

        load.getTrips().stream()
                .filter(t -> t.getPlannedStartDate() != null)
                .findFirst()
                .ifPresent(t -> load.setLoadingDate(t.getPlannedStartDate()));

        load.getTrips().stream()
                .filter(t -> t.getPlannedEndDate() != null)
                .reduce((first, second) -> second)
                .ifPresent(t -> load.setUnloadingDate(t.getPlannedEndDate()));

        load.getTrips().stream()
                .filter(t -> t.getOriginLocation() != null)
                .findFirst()
                .ifPresent(t -> load.setOriginLocation(t.getOriginLocation()));
        
        load.getTrips().stream()
                .filter(t -> t.getDestinationLocation() != null)
                .findFirst()
                .ifPresent(t -> load.setDestinationLocation(t.getDestinationLocation()));

        boolean allCompleted = load.getTrips().stream()
                .allMatch(t -> TRIP_STATUS_COMPLETED.equals(t.getStatus()) || TRIP_STATUS_FINALIZED.equals(t.getStatus()));
        
        if (allCompleted) {
            load.setStatus(STATUS_COMPLETED);
        } else {
            boolean anyStarted = load.getTrips().stream()
                    .anyMatch(t -> TRIP_STATUS_IN_PROGRESS.equals(t.getStatus()));
            if (anyStarted) {
                load.setStatus(STATUS_IN_TRANSIT);
            } else {
                load.setStatus(STATUS_PENDING);
            }
        }
    }

    private TripSummaryDTO createTripSummaryDTO(Trip trip) {
        String vehicleReg = null;
        if (trip.getVehicle() != null) {
            vehicleReg = trip.getVehicle().getRegistrationNumber();
        }
        
        String driverName = null;
        if (trip.getDriver() != null) {
            String firstName = trip.getDriver().getFirstName() != null ? trip.getDriver().getFirstName() : "";
            String lastName = trip.getDriver().getLastName() != null ? trip.getDriver().getLastName() : "";
            driverName = (firstName + " " + lastName).trim();
            if (driverName.isEmpty()) {
                driverName = null;
            }
        }

        return TripSummaryDTO.builder()
                .id(trip.getId())
                .tripNumber(trip.getTripNumber())
                .referenceNumber(trip.getReferenceNumber())
                .status(trip.getStatus())
                .originLocation(trip.getOriginLocation())
                .destinationLocation(trip.getDestinationLocation())
                .originCity(trip.getOriginCity())
                .destinationCity(trip.getDestinationCity())
                .originZipCode(trip.getOriginZipCode())
                .destinationZipCode(trip.getDestinationZipCode())
                .vehicleRegistration(vehicleReg)
                .driverName(driverName)
                .plannedStartDate(trip.getPlannedStartDate())
                .plannedEndDate(trip.getPlannedEndDate())
                .commodityType(trip.getCommodityType())
                .cargoWeight(trip.getCargoWeight())
                .palletCount(trip.getPalletCount())
                .containerNumber(trip.getContainerNumber())
                .fromDepotKm(trip.getFromDepotKm())
                .toDepotKm(trip.getToDepotKm())
                .customerId(trip.getCustomerId())
                .build();
    }

    private LoadResponseDTO mapToResponseDTO(Load load) {
        String customerName = null;
        if (load.getCustomerId() != null) {
            Customer customer = customerRepository.findById(load.getCustomerId()).orElse(null);
            if (customer != null) {
                customerName = customer.getName();
            }
        }

        List<TripSummaryDTO> tripSummaries = new ArrayList<>();
        if (load.getTrips() != null && !load.getTrips().isEmpty()) {
            for (Trip trip : load.getTrips()) {
                tripSummaries.add(createTripSummaryDTO(trip));
            }
        }

        return LoadResponseDTO.builder()
                .id(load.getId())
                .loadNumber(load.getLoadNumber())
                .referenceNumber(load.getReferenceNumber())
                .description(load.getDescription())
                .customerId(load.getCustomerId())
                .customerName(customerName)
                .weightKg(load.getWeightKg())
                .volumeCubicM(load.getVolumeCubicM())
                .loadingDate(load.getLoadingDate())
                .unloadingDate(load.getUnloadingDate())
                .status(load.getStatus())
                .commodityType(load.getCommodityType())
                .palletCount(load.getPalletCount())
                .containerNumber(load.getContainerNumber())
                .hazardousMaterial(load.getHazardousMaterial())
                .specialHandling(load.getSpecialHandling())
                .estimatedValue(load.getEstimatedValue())
                .actualValue(load.getActualValue())
                .priority(load.getPriority())
                .tripsCount(load.getTrips() != null ? load.getTrips().size() : 0)
                .trips(tripSummaries)
                .createdAt(load.getCreatedAt())
                .updatedAt(load.getUpdatedAt())
                .originLocation(load.getOriginLocation())
                .destinationLocation(load.getDestinationLocation())
                .handlingInstructions(load.getHandlingInstructions())
                .packagingType(load.getPackagingType())
                .hazardClass(load.getHazardClass())
                .temperatureRequirements(load.getTemperatureRequirements())
                .totalDistanceKm(load.getTotalDistanceKm())
                .totalHoursActive(load.getTotalHoursActive())
                .incidentsLogged(load.getIncidentsLogged())
                .completedTrips(load.getCompletedTrips())
                .pendingTrips(load.getTrips() != null ? 
                    (int) load.getTrips().stream()
                        .filter(t -> t.getStatus() != null && TRIP_STATUS_PLANNED.equals(t.getStatus()))
                        .count() : 0)
                .inProgressTrips(load.getTrips() != null ? 
                    (int) load.getTrips().stream()
                        .filter(t -> t.getStatus() != null && TRIP_STATUS_IN_PROGRESS.equals(t.getStatus()))
                        .count() : 0)
                .insurancePolicyNumber(load.getInsurancePolicyNumber())
                .insuranceExpiry(load.getInsuranceExpiry())
                .customsClearanceStatus(load.getCustomsClearanceStatus())
                .warehouseId(load.getWarehouseId())
                .supervisorId(load.getSupervisorId())
                .lastStatusUpdate(load.getLastStatusUpdate())
                .auditTrail(load.getAuditTrail())
                .totalFromDepotKm(load.getTotalFromDepotKm())
                .totalToDepotKm(load.getTotalToDepotKm())
                .totalDepotKm(load.getTotalDepotKm())
                .totalWeight(load.getTotalWeight())
                .totalValue(load.getTotalValue())
                .statusDisplay(load.getStatusDisplay())
                .isActive(load.isActive())
                .canAcceptTrip(load.canAcceptTrip())
                .mergeSuggestion(false)
                .build();
    }
}
