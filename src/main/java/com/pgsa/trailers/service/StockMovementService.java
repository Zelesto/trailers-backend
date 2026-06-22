// src/main/java/com/pgsa/trailers/service/inventory/StockMovementService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.StockMovementRequestDTO;
import com.pgsa.trailers.dto.StockMovementResponseDTO;
import com.pgsa.trailers.entity.inventory.InventoryItem;
import com.pgsa.trailers.entity.inventory.StockMovement;
import com.pgsa.trailers.repository.InventoryItemRepository;
import com.pgsa.trailers.repository.StockMovementRepository;
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
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final InventoryItemRepository inventoryItemRepository;

    public StockMovementResponseDTO recordMovement(StockMovementRequestDTO request) {
        // Validate item exists
        InventoryItem item = inventoryItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new RuntimeException("Inventory item not found with ID: " + request.getItemId()));

        // Validate movement type
        if (!request.getMovementType().matches("IN|OUT|ADJUSTMENT")) {
            throw new RuntimeException("Invalid movement type. Use IN, OUT, or ADJUSTMENT");
        }

        // Create movement record using builder
        StockMovement movement = StockMovement.builder()
                .itemId(request.getItemId())
                .quantity(request.getQuantity())
                .movementType(request.getMovementType())
                .reason(request.getReason())
                .notes(request.getNotes())
                .referenceNumber(request.getReferenceNumber())
                .tripId(request.getTripId())
                .fuelSlipId(request.getFuelSlipId())
                .build();

        // Update inventory quantity
        int currentQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
        int newQuantity;

        switch (request.getMovementType()) {
            case "IN":
                newQuantity = currentQuantity + request.getQuantity();
                break;
            case "OUT":
                if (currentQuantity < request.getQuantity()) {
                    throw new RuntimeException("Insufficient stock. Available: " + currentQuantity + ", Requested: " + request.getQuantity());
                }
                newQuantity = currentQuantity - request.getQuantity();
                break;
            case "ADJUSTMENT":
                newQuantity = request.getQuantity();
                break;
            default:
                throw new RuntimeException("Invalid movement type");
        }

        item.setQuantity(newQuantity);
        inventoryItemRepository.save(item);

        StockMovement saved = stockMovementRepository.save(movement);
        log.info("Recorded stock movement for item {}: {} {} units", 
                request.getItemId(), request.getMovementType(), request.getQuantity());

        return mapToResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementResponseDTO> getMovementsByItem(Long itemId, Pageable pageable) {
        return stockMovementRepository.findByItemId(itemId, pageable)
                .map(this::mapToResponseDTO);
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponseDTO> getMovementsByTrip(Long tripId) {
        return stockMovementRepository.findByTripId(tripId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponseDTO> getMovementsByFuelSlip(Long fuelSlipId) {
        return stockMovementRepository.findByFuelSlipId(fuelSlipId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockMovementResponseDTO> getMovementsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return stockMovementRepository.findByDateRange(startDate, endDate)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    private StockMovementResponseDTO mapToResponseDTO(StockMovement movement) {
    // Create a separate variable for the item name
    String itemName = null;
    Long itemId = movement.getItemId(); // Store in a local variable first
    
    inventoryItemRepository.findById(itemId)
            .ifPresent(item -> {
                // Now item is effectively final
                String name = item.getName();
                // Use the name
            });
    
    // Better approach - use Optional directly
    java.util.Optional<InventoryItem> optionalItem = inventoryItemRepository.findById(movement.getItemId());
    if (optionalItem.isPresent()) {
        itemName = optionalItem.get().getName();
    }

    return StockMovementResponseDTO.builder()
            .id(movement.getId())
            .itemId(movement.getItemId())
            .itemName(itemName)
            .quantity(movement.getQuantity())
            .movementType(movement.getMovementType())
            .reason(movement.getReason())
            .notes(movement.getNotes())
            .referenceNumber(movement.getReferenceNumber())
            .performedBy(movement.getPerformedBy())
            .createdAt(movement.getCreatedAt())
            .tripId(movement.getTripId())
            .fuelSlipId(movement.getFuelSlipId())
            .build();
}
}
