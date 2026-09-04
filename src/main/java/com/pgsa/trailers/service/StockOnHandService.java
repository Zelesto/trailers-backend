// src/main/java/com/pgsa/trailers/service/StockOnHandService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.StockOnHandDTO;
import com.pgsa.trailers.dto.StockOnHandFilterDTO;
import com.pgsa.trailers.entity.inventory.InventoryItem;
import com.pgsa.trailers.entity.inventory.InventoryLocation;
import com.pgsa.trailers.entity.inventory.VehicleIssue;
import com.pgsa.trailers.entity.inventory.VehicleIssueItem;
import com.pgsa.trailers.repository.InventoryItemRepository;
import com.pgsa.trailers.repository.InventoryLocationRepository;
import com.pgsa.trailers.repository.VehicleIssueItemRepository;
import com.pgsa.trailers.repository.VehicleIssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StockOnHandService {

    private static final String STATUS_ISSUED = "ISSUED";
    private static final String STATUS_RETURNED = "RETURNED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final VehicleIssueRepository vehicleIssueRepository;
    private final VehicleIssueItemRepository vehicleIssueItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLocationRepository inventoryLocationRepository;

    public List<StockOnHandDTO> getStockOnHand(StockOnHandFilterDTO filter) {
        log.info("📊 Fetching stock on hand with filter: {}", filter);
        
        List<StockOnHandDTO> results = new ArrayList<>();
        
        // 1. Get all active issues with outstanding items
        List<VehicleIssue> activeIssues = vehicleIssueRepository.findByStatusNotIn(
                List.of(STATUS_RETURNED, STATUS_CANCELLED)
        );
        
        log.info("📋 Found {} active issues", activeIssues.size());
        
        for (VehicleIssue issue : activeIssues) {
            List<VehicleIssueItem> items = vehicleIssueItemRepository.findByIssueId(issue.getId());
            
            for (VehicleIssueItem issueItem : items) {
                BigDecimal outstanding = issueItem.getQuantityIssued()
                        .subtract(issueItem.getQuantityReturned());
                
                // Skip if no outstanding quantity
                if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                
                // Determine holder type and name
                String holderType;
                String holderName;
                String holderIdentifier;
                Long holderId;
                
                if (issue.getVehicleId() != null) {
                    holderType = "VEHICLE";
                    holderId = issue.getVehicleId();
                    holderIdentifier = "Vehicle #" + holderId;
                    holderName = "Vehicle #" + holderId;
                } else if (issue.getDriverId() != null) {
                    holderType = "DRIVER";
                    holderId = issue.getDriverId();
                    holderIdentifier = "Driver #" + holderId;
                    holderName = "Driver #" + holderId;
                } else {
                    continue; // Skip if no holder
                }
                
                // Apply filters
                if (!"ALL".equals(filter.getHolderType()) && 
                    !filter.getHolderType().equals(holderType)) {
                    continue;
                }
                
                if (filter.getHolderId() != null && !filter.getHolderId().equals(holderId)) {
                    continue;
                }
                
                if (filter.getItemId() != null && !filter.getItemId().equals(issueItem.getItemId())) {
                    continue;
                }
                
                InventoryItem inventoryItem = inventoryItemRepository.findById(issueItem.getItemId())
                        .orElse(null);
                if (inventoryItem == null) continue;
                
                if (!"ALL".equals(filter.getCategory()) && filter.getCategory() != null &&
                    !filter.getCategory().equals(inventoryItem.getCategory())) {
                    continue;
                }
                
                if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                    String searchLower = filter.getSearch().toLowerCase();
                    boolean matches = (inventoryItem.getName() != null && 
                                      inventoryItem.getName().toLowerCase().contains(searchLower)) ||
                                     (inventoryItem.getSku() != null && 
                                      inventoryItem.getSku().toLowerCase().contains(searchLower)) ||
                                     holderName.toLowerCase().contains(searchLower);
                    if (!matches) continue;
                }
                
                // Determine status
                String status = determineStockStatus(inventoryItem);
                if (!"ALL".equals(filter.getStatus()) && !filter.getStatus().equals(status)) {
                    continue;
                }
                
                // Build DTO
                StockOnHandDTO dto = StockOnHandDTO.builder()
                        .id(issueItem.getId())
                        .itemId(issueItem.getItemId())
                        .itemName(inventoryItem.getName())
                        .itemSku(inventoryItem.getSku())
                        .category(inventoryItem.getCategory())
                        .unitOfMeasure(inventoryItem.getUnitOfMeasure())
                        .holderType(holderType)
                        .holderId(holderId)
                        .holderName(holderName)
                        .holderIdentifier(holderIdentifier)
                        .quantityOnHand(inventoryItem.getQuantity() != null ? inventoryItem.getQuantity() : 0)
                        .quantityIssued(issueItem.getQuantityIssued().intValue())
                        .quantityReturned(issueItem.getQuantityReturned().intValue())
                        .quantityOutstanding(outstanding.intValue())
                        .unitCost(inventoryItem.getUnitCost())
                        .totalValue(inventoryItem.getUnitCost() != null ? 
                            inventoryItem.getUnitCost().multiply(BigDecimal.valueOf(outstanding.intValue())) : 
                            BigDecimal.ZERO)
                        .status(status)
                        .isHeld(inventoryItem.getIsHeld() != null ? inventoryItem.getIsHeld() : false)
                        .holdReason(inventoryItem.getHoldReason())
                        .condition(issueItem.getConditionIssued())
                        .issueDate(issue.getIssueDate())
                        .lastUpdated(issue.getUpdatedAt())
                        .notes(issueItem.getNotes())
                        .build();
                
                results.add(dto);
            }
        }
        
        // 2. Add warehouse/location stock for items not issued
        addLocationStock(results, filter);
        
        log.info("📊 Found {} stock on hand records", results.size());
        return results;
    }

    private void addLocationStock(List<StockOnHandDTO> results, StockOnHandFilterDTO filter) {
        // Only add location stock if not filtering by specific holder type (except LOCATION)
        if (!"ALL".equals(filter.getHolderType()) && !"LOCATION".equals(filter.getHolderType())) {
            return;
        }
        
        List<InventoryItem> allItems = inventoryItemRepository.findAll();
        
        // Get IDs of items that are already in results (issued)
        Set<Long> issuedItemIds = results.stream()
                .map(StockOnHandDTO::getItemId)
                .collect(Collectors.toSet());
        
        for (InventoryItem inventoryItem : allItems) {
            // Skip if item is already issued (already in results)
            if (issuedItemIds.contains(inventoryItem.getId())) {
                continue;
            }
            
            // Skip if item has no stock
            if (inventoryItem.getQuantity() == null || inventoryItem.getQuantity() <= 0) {
                continue;
            }
            
            // Apply filters
            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchLower = filter.getSearch().toLowerCase();
                boolean matches = (inventoryItem.getName() != null && 
                                  inventoryItem.getName().toLowerCase().contains(searchLower)) ||
                                 (inventoryItem.getSku() != null && 
                                  inventoryItem.getSku().toLowerCase().contains(searchLower));
                if (!matches) continue;
            }
            
            if (!"ALL".equals(filter.getCategory()) && filter.getCategory() != null &&
                !filter.getCategory().equals(inventoryItem.getCategory())) {
                continue;
            }
            
            // Get location name
            String locationName = "Warehouse";
            if (inventoryItem.getLocationId() != null) {
                Optional<InventoryLocation> location = inventoryLocationRepository.findById(
                        inventoryItem.getLocationId()
                );
                if (location.isPresent()) {
                    locationName = location.get().getName();
                }
            }
            
            String status = determineStockStatus(inventoryItem);
            if (!"ALL".equals(filter.getStatus()) && !filter.getStatus().equals(status)) {
                continue;
            }
            
            // Build DTO for location stock
            StockOnHandDTO dto = StockOnHandDTO.builder()
                    .id(inventoryItem.getId())
                    .itemId(inventoryItem.getId())
                    .itemName(inventoryItem.getName())
                    .itemSku(inventoryItem.getSku())
                    .category(inventoryItem.getCategory())
                    .unitOfMeasure(inventoryItem.getUnitOfMeasure())
                    .holderType("LOCATION")
                    .holderId(inventoryItem.getLocationId())
                    .holderName(locationName)
                    .holderIdentifier(locationName)
                    .quantityOnHand(inventoryItem.getQuantity())
                    .quantityIssued(0)
                    .quantityReturned(0)
                    .quantityOutstanding(0)
                    .unitCost(inventoryItem.getUnitCost())
                    .totalValue(inventoryItem.getUnitCost() != null ? 
                        inventoryItem.getUnitCost().multiply(BigDecimal.valueOf(inventoryItem.getQuantity())) : 
                        BigDecimal.ZERO)
                    .status(status)
                    .isHeld(inventoryItem.getIsHeld() != null ? inventoryItem.getIsHeld() : false)
                    .holdReason(inventoryItem.getHoldReason())
                    .condition("AVAILABLE")
                    .lastUpdated(inventoryItem.getUpdatedAt())
                    .notes(inventoryItem.getNotes())
                    .build();
            
            results.add(dto);
        }
    }

    private String determineStockStatus(InventoryItem item) {
        if (item.getIsHeld() != null && item.getIsHeld()) {
            return "ON_HOLD";
        }
        Integer quantity = item.getQuantity() != null ? item.getQuantity() : 0;
        if (quantity <= 0) {
            return "OUT_OF_STOCK";
        }
        Integer minLevel = item.getMinLevel() != null ? item.getMinLevel() : 0;
        if (quantity <= minLevel) {
            return "LOW_STOCK";
        }
        return "IN_STOCK";
    }
}
