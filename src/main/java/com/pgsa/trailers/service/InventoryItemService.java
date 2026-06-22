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

    public InventoryItemResponseDTO createItem(InventoryItemRequestDTO request) {
        InventoryItem item = new InventoryItem();
        item.setName(request.getName());
        item.setCategory(request.getCategory());
        item.setUnitOfMeasure(request.getUnitOfMeasure());
        item.setIsConsumable(request.getIsConsumable() != null ? request.getIsConsumable() : false);
        item.setReorderLevel(request.getReorderLevel());
        item.setLocationId(request.getLocationId());

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
        item.setLocationId(request.getLocationId());

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
                .activeItems(totalItems) // Assuming all items are active
                .categoryCounts(categoryCounts)
                .locationCounts(locationCounts)
                .averageReorderLevel(averageReorderLevel)
                .build();
    }

    private InventoryItemResponseDTO mapToResponseDTO(InventoryItem item) {
        String locationName = null;
        if (item.getLocationId() != null) {
            inventoryLocationRepository.findById(item.getLocationId())
                    .ifPresent(loc -> locationName = loc.getName());
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
                .build();
    }
}
