package com.pgsa.trailers.service.inventory;

import com.pgsa.trailers.dto.*;
import com.pgsa.trailers.entity.inventory.*;
import com.pgsa.trailers.exception.EntityNotFoundException;
import com.pgsa.trailers.exception.InsufficientStockException;
import com.pgsa.trailers.exception.InvalidOperationException;
import com.pgsa.trailers.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VehicleIssueService {

    private static final String ISSUE_NUMBER_PREFIX = "ISS-";
    private static final String STATUS_ISSUED = "ISSUED";
    private static final String STATUS_RETURNED = "RETURNED";
    private static final String STATUS_PARTIALLY_RETURNED = "PARTIALLY_RETURNED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final VehicleIssueRepository vehicleIssueRepository;
    private final VehicleIssueItemRepository vehicleIssueItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLocationRepository inventoryLocationRepository;
    private final StockMovementRepository stockMovementRepository;

    // ==================== Core Business Methods ====================

    /**
     * Issue items to a vehicle
     */
    @Transactional
    public VehicleIssueResponseDTO issueItemsToVehicle(VehicleIssueRequestDTO request, Long userId) {
        log.info("🚗 Issuing items to vehicle: {}", request.getVehicleId());

        // Validate request
        validateIssueRequest(request);

        // Create and save the issue
        VehicleIssue issue = createVehicleIssue(request, userId);
        
        // Process all items
        List<VehicleIssueItem> issueItems = new ArrayList<>();
        for (VehicleIssueItemRequestDTO itemReq : request.getItems()) {
            VehicleIssueItem issueItem = processIssueItem(issue, itemReq, userId);
            issueItems.add(issueItem);
        }

        log.info("✅ Items issued successfully. Issue Number: {}, Items: {}", 
            issue.getIssueNumber(), issueItems.size());
        return mapToResponseDTO(issue);
    }

    /**
     * Return items from vehicle
     */
    @Transactional
    public VehicleIssueResponseDTO returnItemsFromVehicle(Long issueId, List<ReturnItemRequestDTO> returns, Long userId) {
        log.info("🔄 Returning items from vehicle issue: {}", issueId);

        // Validate and fetch issue
        VehicleIssue issue = getIssueOrThrow(issueId);
        
        // Build lookup map for issue items
        Map<Long, VehicleIssueItem> issueItemMap = buildIssueItemMap(issueId);
        
        // Validate all return items exist
        validateReturnItems(returns, issueItemMap, issueId);

        // Process each return
        for (ReturnItemRequestDTO returnReq : returns) {
            VehicleIssueItem issueItem = issueItemMap.get(returnReq.getItemId());
            processReturnItem(issueItem, returnReq, issue, userId);
        }

        // Update issue status
        updateIssueStatus(issue);

        log.info("✅ Items returned successfully. Issue Number: {}", issue.getIssueNumber());
        return mapToResponseDTO(issue);
    }

    /**
     * Swap an item - return damaged and issue replacement
     * Supports both vehicle and driver swaps based on issueType
     */
    @Transactional
    public VehicleIssueResponseDTO swapItem(Long oldIssueId, SwapItemRequestDTO swapRequest, Long userId) {
        log.info("🔄 Swapping item from issue: {}, Type: {}", oldIssueId, swapRequest.getIssueType());
        
        // Validate request
        validateSwapRequest(swapRequest);
        
        // Fetch old issue and item
        VehicleIssue oldIssue = getIssueOrThrow(oldIssueId);
        VehicleIssueItem oldItem = getIssueItemOrThrow(oldIssueId, swapRequest.getOldItemId());
        
        // Validate swap conditions
        validateSwapConditions(oldItem, swapRequest);
        
        // Process the swap
        return executeSwap(oldIssue, oldItem, swapRequest, userId);
    }

    /**
     * Cancel a vehicle issue
     */
    @Transactional
    public VehicleIssueResponseDTO cancelIssue(Long issueId, String reason, Long userId) {
        log.info("❌ Cancelling vehicle issue: {}", issueId);
        
        VehicleIssue issue = getIssueOrThrow(issueId);
        
        if (STATUS_RETURNED.equals(issue.getStatus())) {
            throw new InvalidOperationException("Cannot cancel a fully returned issue");
        }
        
        // Return all items that haven't been returned
        List<VehicleIssueItem> items = vehicleIssueItemRepository.findByIssueId(issueId);
        
        for (VehicleIssueItem item : items) {
            BigDecimal outstanding = item.getQuantityIssued().subtract(item.getQuantityReturned());
            if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
                // Return outstanding items to inventory
                InventoryItem inventoryItem = findInventoryItemOrThrow(item.getItemId());
                inventoryItem.setQuantity(inventoryItem.getQuantity() + outstanding.intValue());
                inventoryItemRepository.save(inventoryItem);
                
                // Mark as returned
                item.setQuantityReturned(item.getQuantityIssued());
                item.setConditionReturned("CANCELLED");
                item.setUpdatedAt(LocalDateTime.now());
                vehicleIssueItemRepository.save(item);
            }
        }
        
        issue.setStatus(STATUS_CANCELLED);
        issue.setNotes(issue.getNotes() + " | Cancelled: " + reason);
        issue.setUpdatedAt(LocalDateTime.now());
        issue.setUpdatedBy(userId);
        vehicleIssueRepository.save(issue);
        
        log.info("✅ Vehicle issue cancelled: {}", issue.getIssueNumber());
        return mapToResponseDTO(issue);
    }

    // ==================== Query Methods ====================

    @Transactional(readOnly = true)
    public List<VehicleIssueResponseDTO> getAllVehicleIssues() {
        log.info("📋 Fetching all vehicle issues");
        try {
            List<VehicleIssue> issues = vehicleIssueRepository.findAllByOrderByIssueDateDesc();
            log.info("📋 Found {} vehicle issues", issues.size());
            return issues.stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ Error fetching vehicle issues: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public List<VehicleIssueResponseDTO> getIssuesByVehicle(Long vehicleId) {
        log.info("🚗 Fetching issues for vehicle: {}", vehicleId);
        return vehicleIssueRepository.findByVehicleIdOrderByIssueDateDesc(vehicleId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VehicleIssueResponseDTO> getIssuesByDriver(Long driverId) {
        log.info("👤 Fetching issues for driver: {}", driverId);
        return vehicleIssueRepository.findByDriverIdOrderByIssueDateDesc(driverId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VehicleIssueResponseDTO> getActiveIssues() {
        log.info("📋 Fetching active vehicle issues");
        return vehicleIssueRepository.findByStatusNotIn(List.of(STATUS_RETURNED, STATUS_CANCELLED))
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VehicleIssueResponseDTO getIssueById(Long issueId) {
        log.info("📋 Fetching vehicle issue: {}", issueId);
        VehicleIssue issue = getIssueOrThrow(issueId);
        return mapToResponseDTO(issue);
    }

    @Transactional(readOnly = true)
    public List<VehicleIssueItemResponseDTO> getIssueItems(Long issueId) {
        log.info("📦 Fetching items for issue: {}", issueId);
        getIssueOrThrow(issueId); // Validate issue exists
        return vehicleIssueItemRepository.findByIssueId(issueId)
                .stream()
                .map(this::mapItemToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getIssueSummary(Long issueId) {
        log.info("📊 Fetching summary for issue: {}", issueId);
        VehicleIssue issue = getIssueOrThrow(issueId);
        List<VehicleIssueItem> items = vehicleIssueItemRepository.findByIssueId(issueId);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("issue", mapToResponseDTO(issue));
        summary.put("totalItems", items.size());
        summary.put("totalIssued", items.stream()
            .map(VehicleIssueItem::getQuantityIssued)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("totalReturned", items.stream()
            .map(VehicleIssueItem::getQuantityReturned)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("outstandingItems", items.stream()
            .filter(item -> item.getQuantityIssued().compareTo(item.getQuantityReturned()) > 0)
            .count());
        summary.put("returnPercentage", calculateReturnPercentage(items));
        
        return summary;
    }

    // ==================== Private Helper Methods ====================

    private void validateIssueRequest(VehicleIssueRequestDTO request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new InvalidOperationException("At least one item must be issued");
        }
        
        for (VehicleIssueItemRequestDTO itemReq : request.getItems()) {
            InventoryItem item = findInventoryItemOrThrow(itemReq.getItemId());
            
            if (item.getQuantity() < itemReq.getQuantity().intValue()) {
                throw new InsufficientStockException(
                    String.format("Insufficient stock for item: %s. Available: %d, Requested: %d",
                        item.getName(), item.getQuantity(), itemReq.getQuantity().intValue())
                );
            }
            
            if (!Boolean.TRUE.equals(item.getIsVehicleIssuable())) {
                throw new InvalidOperationException(
                    String.format("Item %s is not issuable to vehicles", item.getName())
                );
            }
        }
    }

    private void validateSwapRequest(SwapItemRequestDTO swapRequest) {
        if (swapRequest.getOldItemId() == null) {
            throw new InvalidOperationException("Old item ID is required");
        }
        if (swapRequest.getNewItemId() == null) {
            throw new InvalidOperationException("New item ID is required");
        }
        if (swapRequest.getNewQuantity() == null || swapRequest.getNewQuantity() <= 0) {
            throw new InvalidOperationException("New quantity must be greater than 0");
        }
        if (swapRequest.getDamagedCondition() == null || swapRequest.getDamagedCondition().trim().isEmpty()) {
            throw new InvalidOperationException("Damaged condition is required");
        }
    }

    private void validateSwapConditions(VehicleIssueItem oldItem, SwapItemRequestDTO swapRequest) {
        // Check if already returned
        if (oldItem.getQuantityReturned().compareTo(oldItem.getQuantityIssued()) >= 0) {
            throw new InvalidOperationException("Item already returned, cannot swap");
        }
        
        // Check if already swapped
        if (Boolean.TRUE.equals(oldItem.getIsSwap())) {
            throw new InvalidOperationException("Item has already been swapped");
        }
        
        // Check new item stock
        InventoryItem newItem = findInventoryItemOrThrow(swapRequest.getNewItemId());
        if (newItem.getQuantity() < swapRequest.getNewQuantity()) {
            throw new InsufficientStockException(
                String.format("Insufficient stock for new item: %s. Available: %d, Requested: %d",
                    newItem.getName(), newItem.getQuantity(), swapRequest.getNewQuantity())
            );
        }
        
        // Check if new item is issuable
        if (!Boolean.TRUE.equals(newItem.getIsVehicleIssuable())) {
            throw new InvalidOperationException(
                String.format("Item %s is not issuable to vehicles", newItem.getName())
            );
        }
    }

    private VehicleIssue createVehicleIssue(VehicleIssueRequestDTO request, Long userId) {
        VehicleIssue issue = VehicleIssue.builder()
                .issueNumber(generateIssueNumber())
                .vehicleId(request.getVehicleId())
                .driverId(request.getDriverId())
                .tripId(request.getTripId())
                .issueDate(request.getIssueDate() != null ? request.getIssueDate() : LocalDateTime.now())
                .status(STATUS_ISSUED)
                .notes(request.getNotes())
                .createdBy(userId)
                .updatedBy(userId)
                .build();
        
        return vehicleIssueRepository.save(issue);
    }

    private VehicleIssueItem processIssueItem(VehicleIssue issue, VehicleIssueItemRequestDTO itemReq, Long userId) {
        // Create issue item
        VehicleIssueItem issueItem = VehicleIssueItem.builder()
                .issue(issue)
                .itemId(itemReq.getItemId())
                .quantityIssued(itemReq.getQuantity())
                .quantityReturned(BigDecimal.ZERO)
                .conditionIssued(itemReq.getCondition())
                .notes(itemReq.getNotes())
                .isSwap(false)
                .build();
        
        vehicleIssueItemRepository.save(issueItem);
        
        // Update inventory
        InventoryItem item = findInventoryItemOrThrow(itemReq.getItemId());
        item.setQuantity(item.getQuantity() - itemReq.getQuantity().intValue());
        inventoryItemRepository.save(item);
        
        // Create stock movement
        StockMovement movement = StockMovement.builder()
                .itemId(itemReq.getItemId())
                .quantity(itemReq.getQuantity().intValue())
                .movementType("OUT")
                .reason("Vehicle Issue")
                .notes(String.format("Issued to vehicle: %d, Driver: %d, Trip: %d",
                    issue.getVehicleId(), issue.getDriverId(), issue.getTripId()))
                .referenceNumber(issue.getIssueNumber())
                .performedBy(String.valueOf(userId))
                .tripId(issue.getTripId())
                .referenceType("VEHICLE_ISSUE")
                .requiresApproval(false)
                .approvalStatus("APPROVED")
                .build();
        
        stockMovementRepository.save(movement);
        
        return issueItem;
    }

    private void processReturnItem(VehicleIssueItem issueItem, ReturnItemRequestDTO returnReq, 
                                   VehicleIssue issue, Long userId) {
        // Validate return quantity
        if (returnReq.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Return quantity must be greater than 0");
        }
        
        BigDecimal remainingOutstanding = issueItem.getQuantityIssued()
                .subtract(issueItem.getQuantityReturned());
        
        if (returnReq.getQuantity().compareTo(remainingOutstanding) > 0) {
            throw new InvalidOperationException(
                String.format("Cannot return more than outstanding quantity. Outstanding: %s, Requested: %s",
                    remainingOutstanding, returnReq.getQuantity())
            );
        }
        
        // Update return quantity
        issueItem.setQuantityReturned(issueItem.getQuantityReturned().add(returnReq.getQuantity()));
        issueItem.setConditionReturned(returnReq.getCondition());
        issueItem.setUpdatedAt(LocalDateTime.now());
        vehicleIssueItemRepository.save(issueItem);
        
        // Return to inventory
        InventoryItem item = findInventoryItemOrThrow(returnReq.getItemId());
        item.setQuantity(item.getQuantity() + returnReq.getQuantity().intValue());
        inventoryItemRepository.save(item);
        
        // Create stock movement
        StockMovement movement = StockMovement.builder()
                .itemId(returnReq.getItemId())
                .quantity(returnReq.getQuantity().intValue())
                .movementType("IN")
                .reason("Vehicle Return")
                .notes(String.format("Returned from vehicle: %d, Condition: %s",
                    issue.getVehicleId(), returnReq.getCondition()))
                .referenceNumber(issue.getIssueNumber())
                .performedBy(String.valueOf(userId))
                .referenceType("VEHICLE_RETURN")
                .requiresApproval(false)
                .approvalStatus("APPROVED")
                .build();
        
        stockMovementRepository.save(movement);
    }

    private VehicleIssueResponseDTO executeSwap(VehicleIssue oldIssue, VehicleIssueItem oldItem,
                                                SwapItemRequestDTO swapRequest, Long userId) {
        // 1. Process old item return
        BigDecimal returnQuantity = swapRequest.getReturnQuantity() != null ? 
                swapRequest.getReturnQuantity() : oldItem.getQuantityIssued();
        
        // Validate return quantity doesn't exceed issued quantity
        if (returnQuantity.compareTo(oldItem.getQuantityIssued()) > 0) {
            throw new InvalidOperationException(
                String.format("Return quantity (%s) cannot exceed issued quantity (%s)",
                    returnQuantity, oldItem.getQuantityIssued())
            );
        }
        
        oldItem.setQuantityReturned(oldItem.getQuantityReturned().add(returnQuantity));
        oldItem.setConditionReturned(swapRequest.getDamagedCondition());
        oldItem.setIsSwap(true);
        oldItem.setSwapReason(swapRequest.getDamagedCondition() + 
            (swapRequest.getDamageNotes() != null ? ": " + swapRequest.getDamageNotes() : ""));
        oldItem.setUpdatedAt(LocalDateTime.now());
        vehicleIssueItemRepository.save(oldItem);
        
        // 2. Create hold/damage record on inventory item
        InventoryItem inventoryItem = findInventoryItemOrThrow(swapRequest.getOldItemId());
        inventoryItem.setHoldCode(swapRequest.getDamagedCondition());
        inventoryItem.setHoldReason(swapRequest.getDamageNotes());
        inventoryItem.setHoldDate(LocalDateTime.now());
        inventoryItem.setHeldBy(String.valueOf(userId));
        inventoryItem.setQuantity(inventoryItem.getQuantity() + returnQuantity.intValue());
        inventoryItemRepository.save(inventoryItem);
        
        // 3. Create stock movement for return
        StockMovement returnMovement = StockMovement.builder()
                .itemId(swapRequest.getOldItemId())
                .quantity(returnQuantity.intValue())
                .movementType("IN")
                .reason("Vehicle Swap Return - " + swapRequest.getDamagedCondition())
                .notes(String.format("Damaged item returned from vehicle. Hold code: %s, Notes: %s",
                    swapRequest.getDamagedCondition(), 
                    swapRequest.getDamageNotes() != null ? swapRequest.getDamageNotes() : "N/A"))
                .referenceNumber(oldIssue.getIssueNumber())
                .performedBy(String.valueOf(userId))
                .referenceType("VEHICLE_SWAP_RETURN")
                .requiresApproval(false)
                .approvalStatus("APPROVED")
                .build();
        stockMovementRepository.save(returnMovement);
        
        // 4. Create new issue for replacement
        VehicleIssueRequestDTO newIssueRequest = buildSwapIssueRequest(oldIssue, swapRequest);
        VehicleIssueResponseDTO newIssue = issueItemsToVehicle(newIssueRequest, userId);
        
        // 5. Link new issue to old one
        oldItem.setSwapIssueId(newIssue.getId());
        vehicleIssueItemRepository.save(oldItem);
        
        log.info("✅ Vehicle swap completed: Old issue {} returned, New issue {} created", 
            oldIssue.getId(), newIssue.getId());
        
        return newIssue;
    }

    private VehicleIssueRequestDTO buildSwapIssueRequest(VehicleIssue oldIssue, SwapItemRequestDTO swapRequest) {
        VehicleIssueRequestDTO request = new VehicleIssueRequestDTO();
        request.setVehicleId(oldIssue.getVehicleId());
        request.setDriverId(oldIssue.getDriverId());
        request.setTripId(oldIssue.getTripId());
        request.setIssueDate(LocalDateTime.now());
        request.setNotes(String.format("SWAP: Replacing damaged item. Original Issue: %s, Damage: %s",
            oldIssue.getIssueNumber(), swapRequest.getDamagedCondition()));
        
        VehicleIssueItemRequestDTO itemRequest = new VehicleIssueItemRequestDTO();
        itemRequest.setItemId(swapRequest.getNewItemId());
        itemRequest.setQuantity(BigDecimal.valueOf(swapRequest.getNewQuantity()));
        itemRequest.setCondition("NEW");
        itemRequest.setNotes(String.format("Swap replacement for %s", swapRequest.getDamagedCondition()));
        
        request.setItems(List.of(itemRequest));
        return request;
    }

    private void validateReturnItems(List<ReturnItemRequestDTO> returns, 
                                     Map<Long, VehicleIssueItem> issueItemMap, 
                                     Long issueId) {
        if (returns == null || returns.isEmpty()) {
            throw new InvalidOperationException("At least one item must be returned");
        }
        
        Set<Long> validItemIds = issueItemMap.keySet();
        
        for (ReturnItemRequestDTO returnReq : returns) {
            if (returnReq.getItemId() == null) {
                throw new InvalidOperationException("Item ID cannot be null");
            }
            if (!validItemIds.contains(returnReq.getItemId())) {
                throw new EntityNotFoundException(
                    String.format("Item %d is not associated with vehicle issue %d. Valid items: %s",
                        returnReq.getItemId(), issueId, validItemIds)
                );
            }
        }
    }

    private Map<Long, VehicleIssueItem> buildIssueItemMap(Long issueId) {
        List<VehicleIssueItem> issueItems = vehicleIssueItemRepository.findByIssueId(issueId);
        if (issueItems.isEmpty()) {
            throw new EntityNotFoundException("No items found for vehicle issue: " + issueId);
        }
        return issueItems.stream()
                .collect(Collectors.toMap(VehicleIssueItem::getItemId, Function.identity()));
    }

    private void updateIssueStatus(VehicleIssue issue) {
        List<VehicleIssueItem> items = vehicleIssueItemRepository.findByIssueId(issue.getId());
        
        if (items.isEmpty()) {
            issue.setStatus(STATUS_CANCELLED);
        } else {
            boolean allReturned = items.stream()
                    .allMatch(item -> item.getQuantityReturned().compareTo(item.getQuantityIssued()) >= 0);
            boolean anyReturned = items.stream()
                    .anyMatch(item -> item.getQuantityReturned().compareTo(BigDecimal.ZERO) > 0);
            
            if (allReturned) {
                issue.setStatus(STATUS_RETURNED);
            } else if (anyReturned) {
                issue.setStatus(STATUS_PARTIALLY_RETURNED);
            } else {
                issue.setStatus(STATUS_ISSUED);
            }
        }
        
        issue.setUpdatedAt(LocalDateTime.now());
        vehicleIssueRepository.save(issue);
    }

    private BigDecimal calculateReturnPercentage(List<VehicleIssueItem> items) {
        if (items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalIssued = items.stream()
                .map(VehicleIssueItem::getQuantityIssued)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalReturned = items.stream()
                .map(VehicleIssueItem::getQuantityReturned)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalIssued.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return totalReturned.divide(totalIssued, 2, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    // ==================== Utility Methods ====================

    private String generateIssueNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5);
        return ISSUE_NUMBER_PREFIX + timestamp;
    }

    private VehicleIssue getIssueOrThrow(Long issueId) {
        return vehicleIssueRepository.findById(issueId)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle issue not found: " + issueId));
    }

    private VehicleIssueItem getIssueItemOrThrow(Long issueId, Long itemId) {
        return vehicleIssueItemRepository
                .findByIssueIdAndItemId(issueId, itemId)
                .orElseThrow(() -> new EntityNotFoundException(
                    String.format("Item %d not found in vehicle issue %d", itemId, issueId)
                ));
    }

    private InventoryItem findInventoryItemOrThrow(Long itemId) {
        return inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found: " + itemId));
    }

    // ==================== Mapping Methods ====================

    private VehicleIssueResponseDTO mapToResponseDTO(VehicleIssue issue) {
        if (issue == null) {
            return null;
        }
        
        List<VehicleIssueItem> items = vehicleIssueItemRepository.findByIssueId(issue.getId());
        
        List<VehicleIssueItemResponseDTO> itemDTOs = items.stream()
                .map(this::mapItemToResponseDTO)
                .collect(Collectors.toList());
        
        return VehicleIssueResponseDTO.builder()
                .id(issue.getId())
                .issueNumber(issue.getIssueNumber())
                .vehicleId(issue.getVehicleId())
                .driverId(issue.getDriverId())
                .tripId(issue.getTripId())
                .issueDate(issue.getIssueDate())
                .status(issue.getStatus())
                .notes(issue.getNotes())
                .items(itemDTOs)
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }

    private VehicleIssueItemResponseDTO mapItemToResponseDTO(VehicleIssueItem item) {
        InventoryItem inventoryItem = inventoryItemRepository.findById(item.getItemId()).orElse(null);
        
        return VehicleIssueItemResponseDTO.builder()
                .id(item.getId())
                .itemId(item.getItemId())
                .itemName(inventoryItem != null ? inventoryItem.getName() : "Unknown")
                .itemCategory(inventoryItem != null ? inventoryItem.getCategory() : null)
                .quantityIssued(item.getQuantityIssued())
                .quantityReturned(item.getQuantityReturned())
                .quantityOutstanding(item.getQuantityIssued().subtract(item.getQuantityReturned()))
                .conditionIssued(item.getConditionIssued())
                .conditionReturned(item.getConditionReturned())
                .isSwap(item.getIsSwap())
                .swapReason(item.getSwapReason())
                .swapIssueId(item.getSwapIssueId())
                .notes(item.getNotes())
                .build();
    }
}
