// src/main/java/com/pgsa/trailers/service/inventory/InventoryLocationService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.InventoryLocationRequestDTO;
import com.pgsa.trailers.dto.InventoryLocationResponseDTO;
import com.pgsa.trailers.entity.InventoryLocation;
import com.pgsa.trailers.repository.InventoryLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InventoryLocationService {

    private final InventoryLocationRepository inventoryLocationRepository;

    @Transactional(readOnly = true)
    public List<InventoryLocationResponseDTO> getAllLocations() {
        return inventoryLocationRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventoryLocationResponseDTO getLocationById(Long id) {
        InventoryLocation location = inventoryLocationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory location not found with ID: " + id));
        return mapToResponseDTO(location);
    }

    public InventoryLocationResponseDTO createLocation(InventoryLocationRequestDTO request) {
        InventoryLocation location = new InventoryLocation();
        location.setName(request.getName());
        location.setType(request.getType());

        InventoryLocation saved = inventoryLocationRepository.save(location);
        log.info("Created inventory location with ID: {}", saved.getId());
        return mapToResponseDTO(saved);
    }

    public InventoryLocationResponseDTO updateLocation(Long id, InventoryLocationRequestDTO request) {
        InventoryLocation location = inventoryLocationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory location not found with ID: " + id));

        location.setName(request.getName());
        location.setType(request.getType());

        InventoryLocation updated = inventoryLocationRepository.save(location);
        log.info("Updated inventory location with ID: {}", updated.getId());
        return mapToResponseDTO(updated);
    }

    public void deleteLocation(Long id) {
        if (!inventoryLocationRepository.existsById(id)) {
            throw new RuntimeException("Inventory location not found with ID: " + id);
        }
        inventoryLocationRepository.deleteById(id);
        log.info("Deleted inventory location with ID: {}", id);
    }

    private InventoryLocationResponseDTO mapToResponseDTO(InventoryLocation location) {
        return InventoryLocationResponseDTO.builder()
                .id(location.getId())
                .name(location.getName())
                .type(location.getType())
                .build();
    }
}
