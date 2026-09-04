package com.pgsa.trailers.service.inventory;

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
import com.pgsa.trailers.service.DriverService;
import com.pgsa.trailers.service.VehicleService;
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
    private final VehicleService vehicleService;
    private final DriverService driverService;

    public List<StockOnHandDTO> getStockOnHand(StockOnHandFilterDTO filter) {
        log.info("📊 Fetching stock on hand with filter: {}", filter);
        
        List<StockOnHandDTO> results = new ArrayList<>();
        
        // Get all active issues with outstanding items
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
                
                // Get holder details
                String holderType;
                String holderName;
                String holderIdentifier;
                String holderDetails;
                Long holderId;
                
                if (issue.getVehicleId() != null) {
                    holderType = "VEHICLE";
                    holderId = issue.getVehicleId();
                    
                    // Get vehicle details - use registration number as identifier
                    var vehicle = vehicleService.getVehicleById(issue.getVehicleId());
                    if (vehicle != null) {
                        holderIdentifier = vehicle.getRegistrationNumber() != null ? 
                                vehicle.getRegistrationNumber() : "Vehicle #" + holderId;
                        holderName = vehicle.getRegistrationNumber() != null ? 
                                vehicle.getRegistrationNumber() : "Vehicle #" + holderId;
                        holderDetails = String.format("%s %s", 
                                vehicle.getMake() != null ? vehicle.getMake() : "", 
                                vehicle.getModel() != null ? vehicle.getModel() : "");
                    } else {
                        holderIdentifier = "Vehicle #" + holderId;
                        holderName = "Vehicle #" + holderId;
                        holderDetails = "";
                    }
                } else if (issue.getDriverId() != null) {
                    holderType = "DRIVER";
                    holderId = issue.getDriverId();
                    
                    // Get driver details
                    var driver = driverService.getDriverById(issue.getDriverId());
                    if (driver != null) {
                        holderIdentifier = driver.getFullName() != null ? 
                                driver.getFullName() : "Driver #" + holderId;
                        holderName = driver.getFullName() != null ? 
                                driver.getFullName() : "Driver #" + holderId;
                        holderDetails = driver.getEmployeeId() != null ? 
                                "Employee #" + driver.getEmployeeId() : "";
                    } else {
                        holderIdentifier = "Driver #" + holderId;
                        holderName = "Driver #" + holderId;
                        holderDetails = "";
                    }
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
                                     holderIdentifier.toLowerCase().contains(searchLower) ||
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
                        .holderDetails(holderDetails)
                        .holderDisplayName(holderIdentifier + (holderDetails != null && !holderDetails.isEmpty() ? 
                                " (" + holderDetails + ")" : ""))
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
        
        log.info("📊 Found {} stock on hand records", results.size());
        return results;
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
