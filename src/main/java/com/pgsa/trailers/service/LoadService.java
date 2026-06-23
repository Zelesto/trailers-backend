// src/main/java/com/pgsa/trailers/service/LoadService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.LoadRequestDTO;
import com.pgsa.trailers.dto.LoadResponseDTO;
import com.pgsa.trailers.dto.TripSummaryDTO;
import com.pgsa.trailers.entity.ops.Customer;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.enums.TripStatus;
import com.pgsa.trailers.repository.CustomerRepository;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    private final LoadRepository loadRepository;
    private final TripRepository tripRepository;
    private final CustomerRepository customerRepository;

    // =============================================
    // CREATE
    // =============================================

    /**
     * Create a new load or suggest merging with existing load
     */
    public LoadResponseDTO createLoad(LoadRequestDTO request, Long userId) {
        log.info("Creating load for customer: {}, date: {}", request.getCustomerId(), request.getLoadingDate());

        // Check for existing loads that could be merged
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

        // Validate customer exists
        if (request.getCustomerId() != null && !customerRepository.existsById(request.getCustomerId())) {
            throw new RuntimeException("Customer not found with ID: " + request.getCustomerId());
        }

        // Generate load number
        String loadNumber = generateLoadNumber();

        Load load = Load.builder()
                .loadNumber(loadNumber)
                .description(request.getDescription())
                .customerId(request.getCustomerId())
                .weightKg(request.getWeightKg())
                .volumeCubicM(request.getVolumeCubicM())
                .loadingDate(request.getLoadingDate())
                .unloadingDate(request.getUnloadingDate())
                .status("PENDING")
                .commodityType(request.getCommodityType())
                .palletCount(request.getPalletCount())
                .containerNumber(request.getContainerNumber())
                .hazardousMaterial(request.getHazardousMaterial())
                .specialHandling(request.getSpecialHandling())
                .estimatedValue(request.getEstimatedValue())
                .actualValue(request.getActualValue())
                .priority(request.getPriority() != null ? request.getPriority() : "NORMAL")
                .createdBy(String.valueOf(userId))
                .build();

        Load saved = loadRepository.save(load);
        log.info("Created load with ID: {}, Number: {}", saved.getId(), saved.getLoadNumber());

        // If there are trips to add to this load
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
    public Page<LoadResponseDTO> getAllLoads(Pageable pageable) {
        return loadRepository.findAll(pageable)
                .map(this::mapToResponseDTO);
    }

    @Transactional(readOnly = true)
    public Page<LoadResponseDTO> searchLoads(String search, Pageable pageable) {
        return loadRepository.searchLoads(search, pageable)
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
        load.setCommodityType(request.getCommodityType());
        load.setPalletCount(request.getPalletCount());
        load.setContainerNumber(request.getContainerNumber());
        load.setHazardousMaterial(request.getHazardousMaterial());
        load.setSpecialHandling(request.getSpecialHandling());
        load.setEstimatedValue(request.getEstimatedValue());
        load.setActualValue(request.getActualValue());
        load.setPriority(request.getPriority());
        load.setUpdatedAt(LocalDateTime.now());
        load.setUpdatedBy(String.valueOf(userId));
        
        Load updated = loadRepository.save(load);
        log.info("Updated load with ID: {}", updated.getId());
        return mapToResponseDTO(updated);
    }

    /**
     * Add trips to an existing load
     */
    @Transactional
    public LoadResponseDTO addTripsToLoad(String loadNumber, List<Long> tripIds, Long userId) {
        log.info("Adding {} trips to load {}", tripIds.size(), loadNumber);
        
        Load load = loadRepository.findByLoadNumber(loadNumber)
                .orElseThrow(() -> new RuntimeException("Load not found with number: " + loadNumber));

        List<Trip> trips = tripRepository.findAllById(tripIds);
        
        // Validate all trips belong to the same customer
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

        // Update load details based on trips
        updateLoadFromTrips(load);

        Load updated = loadRepository.save(load);
        log.info("Added {} trips to load {}", trips.size(), loadNumber);

        return mapToResponseDTO(updated);
    }

    // =============================================
    // DELETE
    // =============================================

    public void deleteLoad(Long id) {
        if (!loadRepository.existsById(id)) {
            throw new RuntimeException("Load not found with ID: " + id);
        }
        loadRepository.deleteById(id);
        log.info("Deleted load with ID: {}", id);
    }

    // =============================================
    // SMART MERGE
    // =============================================

    /**
     * Find a merge candidate load for a customer on a specific date
     */
    @Transactional(readOnly = true)
    public Load findMergeCandidate(Long customerId, LocalDateTime loadingDate) {
        LocalDate date = loadingDate.toLocalDate();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<Load> loads = loadRepository.findByCustomerIdAndLoadingDateBetween(
            customerId, startOfDay, endOfDay);

        return loads.stream()
                .filter(l -> !"COMPLETED".equals(l.getStatus()) && !"CANCELLED".equals(l.getStatus()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Suggest merging trips that could be combined into one load
     */
    @Transactional(readOnly = true)
    public List<Trip> findMergeableTrips(Long customerId, LocalDateTime plannedDate) {
        LocalDate date = plannedDate.toLocalDate();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        return tripRepository.findByCustomerIdAndPlannedStartDateBetweenAndLoadIsNull(
            customerId, startOfDay, endOfDay);
    }

    /**
     * Smart merge: Automatically merge trips for the same customer on the same day
     */
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

        LoadRequestDTO loadRequest = new LoadRequestDTO();
        loadRequest.setCustomerId(customerId);
        loadRequest.setLoadingDate(plannedDate);
        loadRequest.setDescription("Auto-merged load for " + customer.getName() + 
            " - " + plannedDate.toLocalDate());
        loadRequest.setTripIds(mergeableTrips.stream()
            .map(Trip::getId)
            .collect(Collectors.toList()));

        if (!mergeableTrips.isEmpty()) {
            Trip firstTrip = mergeableTrips.get(0);
            loadRequest.setCommodityType(firstTrip.getCommodityType());
        }

        LoadResponseDTO response = createLoad(loadRequest, userId);
        response.setMergeMessage("Successfully merged " + mergeableTrips.size() + 
            " trips into load " + response.getLoadNumber());
        
        return response;
    }

    // =============================================
    // PRIVATE HELPERS
    // =============================================

    /**
     * Update load details based on associated trips
     */
    private void updateLoadFromTrips(Load load) {
        if (load.getTrips() == null || load.getTrips().isEmpty()) {
            return;
        }

        // Calculate total weight
        BigDecimal totalWeight = load.getTrips().stream()
                .map(Trip::getCargoWeight)
                .filter(w -> w != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        load.setWeightKg(totalWeight);

        // Calculate total value
        BigDecimal totalValue = load.getTrips().stream()
                .map(Trip::getCargoValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        load.setActualValue(totalValue);

        // Set loading date from first trip
        load.getTrips().stream()
                .filter(t -> t.getPlannedStartDate() != null)
                .findFirst()
                .ifPresent(t -> load.setLoadingDate(t.getPlannedStartDate()));

        // Set unloading date from last trip
        load.getTrips().stream()
                .filter(t -> t.getPlannedEndDate() != null)
                .reduce((first, second) -> second)
                .ifPresent(t -> load.setUnloadingDate(t.getPlannedEndDate()));

        // Update status based on trips
        boolean allCompleted = load.getTrips().stream()
                .allMatch(t -> t.getStatus() == TripStatus.COMPLETED);
        
        if (allCompleted) {
            load.setStatus("COMPLETED");
        } else {
            boolean anyStarted = load.getTrips().stream()
                    .anyMatch(t -> t.getStatus() == TripStatus.IN_PROGRESS);
            if (anyStarted) {
                load.setStatus("IN_PROGRESS");
            } else {
                load.setStatus("PENDING");
            }
        }
    }

    /**
     * Generate a unique load number
     */
    private String generateLoadNumber() {
        return "LD-" + System.currentTimeMillis();
    }

    /**
     * Map Load entity to LoadResponseDTO
     */
    private LoadResponseDTO mapToResponseDTO(Load load) {
        String customerName = null;
        if (load.getCustomerId() != null) {
            customerRepository.findById(load.getCustomerId())
                    .ifPresent(customer -> customerName = customer.getName());
        }

        List<TripSummaryDTO> tripSummaries = new ArrayList<>();
        if (load.getTrips() != null) {
            tripSummaries = load.getTrips().stream()
                    .map(trip -> TripSummaryDTO.builder()
                            .id(trip.getId())
                            .tripNumber(trip.getTripNumber())
                            .status(trip.getStatus())
                            .originLocation(trip.getOriginLocation())
                            .destinationLocation(trip.getDestinationLocation())
                            .originCity(trip.getOriginCity())
                            .destinationCity(trip.getDestinationCity())
                            .originZipCode(trip.getOriginZipCode())
                            .destinationZipCode(trip.getDestinationZipCode())
                            .vehicleRegistration(trip.getVehicle() != null ? 
                                    trip.getVehicle().getRegistrationNumber() : null)
                            .driverName(trip.getDriver() != null ? 
                                    trip.getDriver().getFirstName() + " " + trip.getDriver().getLastName() : null)
                            .plannedStartDate(trip.getPlannedStartDate())
                            .plannedEndDate(trip.getPlannedEndDate())
                            .commodityType(trip.getCommodityType())
                            .cargoWeight(trip.getCargoWeight())
                            .palletCount(trip.getPalletCount())
                            .containerNumber(trip.getContainerNumber())
                            .build())
                    .collect(Collectors.toList());
        }

        return LoadResponseDTO.builder()
                .id(load.getId())
                .loadNumber(load.getLoadNumber())
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
                .tripCount(load.getTrips() != null ? load.getTrips().size() : 0)
                .trips(tripSummaries)
                .createdAt(load.getCreatedAt())
                .updatedAt(load.getUpdatedAt())
                .build();
    }
}
