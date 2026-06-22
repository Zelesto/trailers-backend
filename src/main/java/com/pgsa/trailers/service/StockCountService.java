// src/main/java/com/pgsa/trailers/service/StockCountService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.InventoryVarianceDTO;
import com.pgsa.trailers.entity.inventory.InventoryItem;
import com.pgsa.trailers.entity.inventory.InventoryLocation;
import com.pgsa.trailers.entity.inventory.StockMovement;
import com.pgsa.trailers.enums.StockMovementType;
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

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final StockMovementRepository stockMovementRepository;

    /**
     * Record a stock movement (from your original controller)
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

        // Set the item and location on the movement
        // NOTE: If StockMovement doesn't have these fields, remove these lines
        // movement.setItem(item);
        // movement.setLocation(location);
        
        // Set movement type - convert String to enum if needed
        String movementTypeStr = movement.getMovementType();
        // If you have an enum, convert it
        // StockMovementType type = StockMovementType.valueOf(movementTypeStr);
        
        // Set reference type and ID if needed
        // movement.setReferenceType("STOCK_COUNT");
        // movement.setReferenceId(1L);

        // Save the movement
        stockMovementRepository.save(movement);
        
        // Update the item quantity
        int currentQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
        int quantity = movement.getQuantity();
        
        if ("IN".equals(movementTypeStr)) {
            item.setQuantity(currentQuantity + quantity);
        } else if ("OUT".equals(movementTypeStr)) {
            item.setQuantity(Math.max(0, currentQuantity - quantity));
        } else if ("ADJUSTMENT".equals(movementTypeStr)) {
            item.setQuantity(quantity);
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

    private int calculateExpectedQuantity(InventoryItem item) {
        // Calculate expected quantity based on initial stock + IN movements - OUT movements
        // This is a simplified version - adjust based on your business logic
        Integer initialQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
        
        // You could also query stock movements to calculate expected quantity
        // List<StockMovement> movements = stockMovementRepository.findByItemId(item.getId());
        // int totalIn = movements.stream().filter(m -> "IN".equals(m.getMovementType())).mapToInt(StockMovement::getQuantity).sum();
        // int totalOut = movements.stream().filter(m -> "OUT".equals(m.getMovementType())).mapToInt(StockMovement::getQuantity).sum();
        // return initialQuantity + totalIn - totalOut;
        
        return initialQuantity;
    }
}
