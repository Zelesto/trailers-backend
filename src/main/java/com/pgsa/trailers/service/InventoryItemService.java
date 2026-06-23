// src/main/java/com/pgsa/trailers/service/inventory/InventoryItemService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.InventoryItemRequestDTO;
import com.pgsa.trailers.dto.InventoryItemResponseDTO;
import com.pgsa.trailers.dto.InventoryStatisticsDTO;
import com.pgsa.trailers.entity.inventory.InventoryItem;
import com.pgsa.trailers.entity.inventory.InventoryLocation;
import com.pgsa.trailers.repository.InventoryItemRepository;
import com.pgsa.trailers.repository.InventoryLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InventoryItemService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLocationRepository inventoryLocationRepository;

    @Transactional(readOnly = true)
    public Page<InventoryItemResponseDTO> getAllItems(Pageable pageable) {
        return inventoryItemRepository.findAll(pageable)
                .map(this::mapToResponseDTO);
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemResponseDTO> searchItems(String search, Pageable pageable) {
        if (search == null || search.trim().isEmpty()) {
            return getAllItems(pageable);
        }
        return inventoryItemRepository.searchItems(search.trim(), pageable)
                .map(this::mapToResponseDTO);
    }

    @Transactional(readOnly = true)
    public InventoryItemResponseDTO getItemById(Long id) {
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found with ID: " + id));
        return mapToResponseDTO(item);
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponseDTO> getItemsByCategory(String category) {
        return inventoryItemRepository.findByCategory(category)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponseDTO> getItemsByLocation(Long locationId) {
        return inventoryItemRepository.findByLocationId(locationId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponseDTO> getConsumableItems() {
        return inventoryItemRepository.findByIsConsumableTrue()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponseDTO> getActiveItems() {
        return inventoryItemRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public InventoryItemResponseDTO updateQuantity(Long id, Integer quantity, String operation) {
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found with ID: " + id));

        int currentQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
        int newQuantity;

        if ("SET".equalsIgnoreCase(operation)) {
            newQuantity = quantity;
        } else if ("ADD".equalsIgnoreCase(operation)) {
            newQuantity = currentQuantity + quantity;
        } else if ("SUBTRACT".equalsIgnoreCase(operation)) {
            if (currentQuantity < quantity) {
                throw new RuntimeException("Insufficient stock. Available: " + currentQuantity + ", Requested: " + quantity);
            }
            newQuantity = currentQuantity - quantity;
        } else {
            throw new RuntimeException("Invalid operation. Use SET, ADD, or SUBTRACT");
        }

        if (newQuantity < 0) {
            throw new RuntimeException("Quantity cannot be negative");
        }

        item.setQuantity(newQuantity);
        InventoryItem updated = inventoryItemRepository.save(item);
        log.info("Updated quantity for item ID: {} from {} to {}", id, currentQuantity, newQuantity);
        return mapToResponseDTO(updated);
    }

    public InventoryItemResponseDTO createItem(InventoryItemRequestDTO request) {
        InventoryItem item = InventoryItem.builder()
                .name(request.getName())
                .category(request.getCategory())
                .unitOfMeasure(request.getUnitOfMeasure())
                .isConsumable(request.getIsConsumable() != null ? request.getIsConsumable() : false)
                .reorderLevel(request.getReorderLevel())
                .locationId(request.getLocationId())
                .quantity(request.getQuantity() != null ? request.getQuantity() : 0)
                .unitCost(request.getUnitCost())
                .minLevel(request.getMinLevel())
                .isActive(true)
                .notes(request.getNotes())
                .build();

        InventoryItem saved = inventoryItemRepository.save(item);
        log.info("Created inventory item with ID: {}", saved.getId());
        return mapToResponseDTO(saved);
    }

    public InventoryItemResponseDTO updateItem(Long id, InventoryItemRequestDTO request) {
        InventoryItem item = inventoryItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inventory item not found with ID: " + id));

        item.setName(request.getName());
        item.setCategory(request.getCategory());
        item.setUnitOfMeasure(request.getUnitOfMeasure());
        item.setIsConsumable(request.getIsConsumable());
        item.setReorderLevel(request.getReorderLevel());
        if (request.getLocationId() != null) item.setLocationId(request.getLocationId());
        if (request.getQuantity() != null) item.setQuantity(request.getQuantity());
        if (request.getUnitCost() != null) item.setUnitCost(request.getUnitCost());
        if (request.getMinLevel() != null) item.setMinLevel(request.getMinLevel());
        if (request.getNotes() != null) item.setNotes(request.getNotes());

        InventoryItem updated = inventoryItemRepository.save(item);
        log.info("Updated inventory item with ID: {}", updated.getId());
        return mapToResponseDTO(updated);
    }

    public void deleteItem(Long id) {
        if (!inventoryItemRepository.existsById(id)) {
            throw new RuntimeException("Inventory item not found with ID: " + id);
        }
        inventoryItemRepository.deleteById(id);
        log.info("Deleted inventory item with ID: {}", id);
    }

    @Transactional(readOnly = true)
    public InventoryStatisticsDTO getStatistics() {
        Long totalItems = inventoryItemRepository.countTotalItems();
        
        // Category counts
        List<Object[]> categoryResults = inventoryItemRepository.countByCategory();
        Map<String, Long> categoryCounts = new HashMap<>();
        for (Object[] result : categoryResults) {
            categoryCounts.put((String) result[0], (Long) result[1]);
        }

        // Location counts
        List<Object[]> locationResults = inventoryItemRepository.countByLocation();
        Map<Long, Long> locationCounts = new HashMap<>();
        for (Object[] result : locationResults) {
            locationCounts.put((Long) result[0], (Long) result[1]);
        }

        // Average reorder level
        Double avgReorderLevel = inventoryItemRepository.averageReorderLevel();
        BigDecimal averageReorderLevel = avgReorderLevel != null ? 
                BigDecimal.valueOf(avgReorderLevel).setScale(2, RoundingMode.HALF_UP) : 
                BigDecimal.ZERO;

        return InventoryStatisticsDTO.builder()
                .totalItems(totalItems)
                .activeItems(totalItems)
                .categoryCounts(categoryCounts)
                .locationCounts(locationCounts)
                .averageReorderLevel(averageReorderLevel)
                .build();
    }

    /**
     * Public method to map InventoryItem entity to Response DTO
     * Used by controller for low-stock and out-of-stock endpoints
     */
    public InventoryItemResponseDTO mapToResponseDTO(InventoryItem item) {
        String locationName = null;
        
        if (item.getLocationId() != null) {
            Optional<InventoryLocation> optionalLocation = 
                    inventoryLocationRepository.findById(item.getLocationId());
            if (optionalLocation.isPresent()) {
                locationName = optionalLocation.get().getName();
            }
        }

        String status = "Unknown";
        if (item.getQuantity() != null) {
            if (item.getQuantity() <= 0) {
                status = "Out of Stock";
            } else if (item.getMinLevel() != null && item.getQuantity() <= item.getMinLevel()) {
                status = "Low Stock";
            } else {
                status = "In Stock";
            }
        }

        return InventoryItemResponseDTO.builder()
                .id(item.getId())
                .name(item.getName())
                .category(item.getCategory())
                .unitOfMeasure(item.getUnitOfMeasure())
                .isConsumable(item.getIsConsumable())
                .reorderLevel(item.getReorderLevel())
                .locationId(item.getLocationId())
                .locationName(locationName)
                .quantity(item.getQuantity())
                .unitCost(item.getUnitCost())
                .minLevel(item.getMinLevel())
                .status(status)
                .notes(item.getNotes())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
