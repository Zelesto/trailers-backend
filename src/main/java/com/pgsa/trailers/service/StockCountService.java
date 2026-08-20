// src/main/java/com/pgsa/trailers/service/StockCountService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.InventoryVarianceDTO;
import com.pgsa.trailers.entity.inventory.InventoryItem;
import com.pgsa.trailers.entity.inventory.InventoryLocation;
import com.pgsa.trailers.entity.inventory.StockMovement;
import com.pgsa.trailers.repository.InventoryItemRepository;
import com.pgsa.trailers.repository.InventoryLocationRepository;
import com.pgsa.trailers.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockCountService {

    // ============================================================
    // CONSTANTS FOR MOVEMENT TYPES (from enum_master table)
    // ============================================================
    public static final String MOVEMENT_TYPE_IN = "IN";
    public static final String MOVEMENT_TYPE_OUT = "OUT";
    public static final String MOVEMENT_TYPE_ADJUSTMENT = "ADJUSTMENT";
    public static final String MOVEMENT_TYPE_RETURN = "RETURN";
    public static final String MOVEMENT_TYPE_TRANSFER = "TRANSFER";

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final StockMovementRepository stockMovementRepository;

    /**
     * Record a stock movement - FIXED: Use String for movement type
     */
    public void recordStockMovement(StockMovement movement) {
        // Validate the item exists
        InventoryItem item = inventoryItemRepository.findById(movement.getItemId())
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));

        // Validate location
        InventoryLocation location = null;
        if (item.getLocationId() != null) {
            location = inventoryLocationRepository.findById(item.getLocationId())
                    .orElse(null);
        }

        // Get movement type as String
        String movementTypeStr = movement.getMovementType();
        if (movementTypeStr == null || movementTypeStr.trim().isEmpty()) {
            throw new RuntimeException("Movement type is required");
        }

        // Set reference type and ID if needed
        // movement.setReferenceType("STOCK_COUNT");
        // movement.setReferenceId(1L);

        // Save the movement
        stockMovementRepository.save(movement);
        
        // Update the item quantity based on movement type
        int currentQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
        int quantity = movement.getQuantity();
        
        // Use String constants for comparison
        if (MOVEMENT_TYPE_IN.equals(movementTypeStr)) {
            item.setQuantity(currentQuantity + quantity);
            log.info("✅ Added {} units to item {}", quantity, movement.getItemId());
        } else if (MOVEMENT_TYPE_OUT.equals(movementTypeStr)) {
            int newQuantity = Math.max(0, currentQuantity - quantity);
            item.setQuantity(newQuantity);
            log.info("✅ Removed {} units from item {}", quantity, movement.getItemId());
        } else if (MOVEMENT_TYPE_ADJUSTMENT.equals(movementTypeStr)) {
            item.setQuantity(quantity);
            log.info("✅ Adjusted item {} to {} units", movement.getItemId(), quantity);
        } else {
            log.warn("⚠️ Unknown movement type: {}, skipping quantity update", movementTypeStr);
        }
        
        inventoryItemRepository.save(item);
        log.info("Stock movement recorded for item: {}", movement.getItemId());
    }

    /**
     * Get shrinkage report
     */
    public InventoryVarianceDTO getShrinkageReport(Long itemId) {
        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        // Calculate expected vs actual
        int expectedQuantity = calculateExpectedQuantity(item);
        int actualQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
        int variance = actualQuantity - expectedQuantity;
        double variancePercentage = expectedQuantity > 0 ? 
                ((double) variance / expectedQuantity) * 100 : 0;

        return InventoryVarianceDTO.builder()
                .itemId(itemId)
                .itemName(item.getName())
                .expectedQuantity(BigDecimal.valueOf(expectedQuantity))
                .actualQuantity(BigDecimal.valueOf(actualQuantity))
                .variance(BigDecimal.valueOf(variance))
                .variancePercentage(BigDecimal.valueOf(variancePercentage))
                .reason("Physical count discrepancy")
                .build();
    }

    /**
     * Calculate expected quantity based on initial stock and movements
     * FIXED: Changed from getInitialQuantity() to getQuantity()
     */
    private int calculateExpectedQuantity(InventoryItem item) {
        // Get initial quantity - use getQuantity() as starting point
        Integer initialQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
        
        try {
            // Query stock movements to calculate expected quantity
            // Note: This assumes your StockMovement entity has itemId and quantity fields
            // Adjust based on your actual entity structure
            Integer totalIn = 0;
            Integer totalOut = 0;
            
            // If you have a method to get movements by item ID
            // List<StockMovement> movements = stockMovementRepository.findByItemId(item.getId());
            // for (StockMovement m : movements) {
            //     String type = m.getMovementType();
            //     if (MOVEMENT_TYPE_IN.equals(type)) {
            //         totalIn += m.getQuantity();
            //     } else if (MOVEMENT_TYPE_OUT.equals(type)) {
            //         totalOut += m.getQuantity();
            //     }
            // }
            
            return initialQuantity + totalIn - totalOut;
        } catch (Exception e) {
            log.warn("Could not calculate expected quantity from movements: {}", e.getMessage());
            return initialQuantity;
        }
    }

    /**
     * Get total IN movements for an item
     */
    public int getTotalInMovements(Long itemId) {
        try {
            // This assumes you have a method in repository
            // return stockMovementRepository.sumQuantityByItemIdAndMovementType(itemId, MOVEMENT_TYPE_IN);
            return 0; // Placeholder
        } catch (Exception e) {
            log.warn("Error getting total IN movements: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Get total OUT movements for an item
     */
    public int getTotalOutMovements(Long itemId) {
        try {
            // This assumes you have a method in repository
            // return stockMovementRepository.sumQuantityByItemIdAndMovementType(itemId, MOVEMENT_TYPE_OUT);
            return 0; // Placeholder
        } catch (Exception e) {
            log.warn("Error getting total OUT movements: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Get total adjustment movements for an item
     */
    public int getTotalAdjustmentMovements(Long itemId) {
        try {
            // return stockMovementRepository.sumQuantityByItemIdAndMovementType(itemId, MOVEMENT_TYPE_ADJUSTMENT);
            return 0; // Placeholder
        } catch (Exception e) {
            log.warn("Error getting total adjustment movements: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Validate movement type
     */
    public boolean isValidMovementType(String movementType) {
        if (movementType == null) {
            return false;
        }
        String upper = movementType.toUpperCase();
        return MOVEMENT_TYPE_IN.equals(upper) || 
               MOVEMENT_TYPE_OUT.equals(upper) || 
               MOVEMENT_TYPE_ADJUSTMENT.equals(upper) || 
               MOVEMENT_TYPE_RETURN.equals(upper) || 
               MOVEMENT_TYPE_TRANSFER.equals(upper);
    }

    /**
     * Get movement type display name
     */
    public String getMovementTypeDisplay(String movementType) {
        if (movementType == null) {
            return "Unknown";
        }
        switch (movementType.toUpperCase()) {
            case MOVEMENT_TYPE_IN:
                return "Stock In";
            case MOVEMENT_TYPE_OUT:
                return "Stock Out";
            case MOVEMENT_TYPE_ADJUSTMENT:
                return "Adjustment";
            case MOVEMENT_TYPE_RETURN:
                return "Return";
            case MOVEMENT_TYPE_TRANSFER:
                return "Transfer";
            default:
                return movementType;
        }
    }
}
