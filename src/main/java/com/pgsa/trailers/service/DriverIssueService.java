package com.pgsa.trailers.service;

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
public class DriverIssueService {

    private static final String ISSUE_NUMBER_PREFIX = "DIS-";
    private static final String STATUS_ISSUED = "ISSUED";
    private static final String STATUS_RETURNED = "RETURNED";
    private static final String STATUS_PARTIALLY_RETURNED = "PARTIALLY_RETURNED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final DriverIssueRepository driverIssueRepository;
    private final DriverIssueItemRepository driverIssueItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final StockMovementRepository stockMovementRepository;

    // ==================== Core Business Methods ====================

    @Transactional
    public DriverIssueResponseDTO issueItemsToDriver(DriverIssueRequestDTO request, Long userId) {
        log.info("👤 Issuing items to driver: {}", request.getDriverId());
        validateIssueRequest(request);

        DriverIssue issue = createDriverIssue(request, userId);
        List<DriverIssueItem> issueItems = new ArrayList<>();

        for (DriverIssueItemRequestDTO itemReq : request.getItems()) {
            DriverIssueItem issueItem = processIssueItem(issue, itemReq, userId);
            issueItems.add(issueItem);
        }

        log.info("✅ Items issued successfully. Issue Number: {}, Items: {}",
            issue.getIssueNumber(), issueItems.size());
        return mapToResponseDTO(issue);
    }

    @Transactional
    public DriverIssueResponseDTO returnItemsFromDriver(Long issueId, List<ReturnItemRequestDTO> returns, Long userId) {
        log.info("🔄 Returning items from driver issue: {}", issueId);

        DriverIssue issue = getIssueOrThrow(issueId);
        List<DriverIssueItem> issueItems = driverIssueItemRepository.findByIssueId(issueId);

        if (issueItems.isEmpty()) {
            throw new EntityNotFoundException("No items found for driver issue: " + issueId);
        }

        Map<Long, DriverIssueItem> issueItemMap = issueItems.stream()
                .collect(Collectors.toMap(
                    DriverIssueItem::getItemId,
                    Function.identity(),
                    (existing, replacement) -> existing
                ));

        validateReturnItems(returns, issueItemMap, issueId);

        for (ReturnItemRequestDTO returnReq : returns) {
            DriverIssueItem issueItem = issueItemMap.get(returnReq.getItemId());
            processReturnItem(issueItem, returnReq, issue, userId);
        }

        updateIssueStatus(issue);

        log.info("✅ Items returned successfully. Issue Number: {}", issue.getIssueNumber());
        return mapToResponseDTO(issue);
    }

    /**
     * Update a driver issue — only updates issueDate, status, notes,
     * and per-item condition/notes. Item quantities are NOT touched.
     */
    @Transactional
    public DriverIssueResponseDTO updateDriverIssue(
            Long issueId,
            DriverIssueRequestDTO request,
            Long userId) {

        DriverIssue issue = getIssueOrThrow(issueId);

        // 1. Metadata
        if (request.getIssueDate() != null) issue.setIssueDate(request.getIssueDate());
        if (request.getNotes() != null) issue.setNotes(request.getNotes());

        // 2. Status — refuse RETURNED if items still outstanding
        if (request.getStatus() != null) {
            String newStatus = request.getStatus().toUpperCase();

            if (STATUS_RETURNED.equals(newStatus)) {
                List<DriverIssueItem> items = driverIssueItemRepository.findByIssueId(issueId);
                boolean stillOutstanding = items.stream().anyMatch(line -> {
                    BigDecimal issued = line.getQuantityIssued();
                    BigDecimal returned = line.getQuantityReturned();
                    if (issued == null) return false;
                    if (returned == null) return true;
                    return returned.compareTo(issued) < 0;
                });
                if (stillOutstanding) {
                    throw new InvalidOperationException(
                        "Cannot set status to RETURNED — items are still outstanding. " +
                        "Return them via POST /{issueId}/return first, then edit again.");
                }
            }

            issue.setStatus(newStatus);
        }

        // 3. Items — update metadata only, matched by itemId
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<DriverIssueItem> issueItems = driverIssueItemRepository.findByIssueId(issueId);
            Map<Long, DriverIssueItem> byItemId = issueItems.stream()
                    .collect(Collectors.toMap(
                            DriverIssueItem::getItemId,
                            Function.identity(),
                            (existing, replacement) -> existing));

            for (DriverIssueItemRequestDTO itemReq : request.getItems()) {
                Long incomingId = itemReq.getItemId();
                if (incomingId == null) continue;

                DriverIssueItem line = byItemId.get(incomingId);
                if (line == null) {
                    log.warn("Driver issue {}: item {} not part of this issue — skipping. " +
                             "Create a new issue to add items.", issueId, incomingId);
                    continue;
                }

                if (itemReq.getCondition() != null) line.setConditionIssued(itemReq.getCondition());
                if (itemReq.getNotes() != null) line.setNotes(itemReq.getNotes());
                line.setUpdatedAt(LocalDateTime.now());
                driverIssueItemRepository.save(line);

                if (itemReq.getQuantity() != null
                        && line.getQuantityIssued() != null
                        && line.getQuantityIssued().compareTo(itemReq.getQuantity()) != 0) {
                    log.warn("Driver issue {}: item {} quantity change from {} to {} IGNORED. " +
                             "Use Return/Swap to change quantities.",
                            issueId, incomingId,
                            line.getQuantityIssued(), itemReq.getQuantity());
                }
            }
        }

        issue.setUpdatedBy(userId);
        issue.setUpdatedAt(LocalDateTime.now());
        driverIssueRepository.save(issue);
        log.info("✅ Driver issue {} updated by user {}", issueId, userId);
        return mapToResponseDTO(issue);
    }

    /**
     * Delete a driver issue — refuses if items are still outstanding.
     */
    @Transactional
    public void deleteDriverIssue(Long issueId, Long userId) {
        DriverIssue issue = getIssueOrThrow(issueId);

        List<DriverIssueItem> items = driverIssueItemRepository.findByIssueId(issueId);

        boolean stillOutstanding = items.stream().anyMatch(line -> {
            BigDecimal issued = line.getQuantityIssued();
            BigDecimal returned = line.getQuantityReturned();
            if (issued == null) return false;
            if (returned == null) return true;
            return returned.compareTo(issued) < 0;
        });

        if (stillOutstanding) {
            throw new InvalidOperationException(
                "Cannot delete this issue — some items are still outstanding. " +
                "Return them via POST /{issueId}/return first, then delete.");
        }

        if (!items.isEmpty()) {
            driverIssueItemRepository.deleteAll(items);
        }
        driverIssueRepository.delete(issue);
        log.info("🗑️ Driver issue {} deleted by user {}", issueId, userId);
    }

    @Transactional
    public DriverIssueResponseDTO swapItem(Long oldIssueId, SwapItemRequestDTO swapRequest, Long userId) {
        log.info("🔄 Swapping item from driver issue: {}", oldIssueId);

        validateSwapRequest(swapRequest);

        DriverIssue oldIssue = getIssueOrThrow(oldIssueId);
        DriverIssueItem oldItem = getIssueItemOrThrow(oldIssueId, swapRequest.getOldItemId());

        validateSwapConditions(oldItem, swapRequest);

        return executeSwap(oldIssue, oldItem, swapRequest, userId);
    }

    // ==================== Query Methods ====================

    @Transactional(readOnly = true)
    public List<DriverIssueResponseDTO> getAllDriverIssues() {
        log.info("📋 Fetching all driver issues");
        try {
            List<DriverIssue> issues = driverIssueRepository.findAllByOrderByIssueDateDesc();
            log.info("📋 Found {} driver issues", issues.size());
            return issues.stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("❌ Error fetching driver issues: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public List<DriverIssueResponseDTO> getIssuesByDriver(Long driverId) {
        log.info("👤 Fetching issues for driver: {}", driverId);
        return driverIssueRepository.findByDriverIdOrderByIssueDateDesc(driverId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DriverIssueResponseDTO getIssueById(Long issueId) {
        log.info("📋 Fetching driver issue: {}", issueId);
        DriverIssue issue = getIssueOrThrow(issueId);
        return mapToResponseDTO(issue);
    }

    // ==================== Private Helpers ====================

    private void validateIssueRequest(DriverIssueRequestDTO request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new InvalidOperationException("At least one item must be issued");
        }

        for (DriverIssueItemRequestDTO itemReq : request.getItems()) {
            InventoryItem item = findInventoryItemOrThrow(itemReq.getItemId());

            if (item.getQuantity() < itemReq.getQuantity().intValue()) {
                throw new InsufficientStockException(
                    String.format("Insufficient stock for item: %s. Available: %d, Requested: %d",
                        item.getName(), item.getQuantity(), itemReq.getQuantity().intValue())
                );
            }

            if (!Boolean.TRUE.equals(item.getIsDriverIssuable())) {
                throw new InvalidOperationException(
                    String.format("Item %s is not issuable to drivers", item.getName())
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

    private void validateSwapConditions(DriverIssueItem oldItem, SwapItemRequestDTO swapRequest) {
        if (oldItem.getQuantityReturned().compareTo(oldItem.getQuantityIssued()) >= 0) {
            throw new InvalidOperationException("Item already returned, cannot swap");
        }

        if (Boolean.TRUE.equals(oldItem.getIsSwap())) {
            throw new InvalidOperationException("Item has already been swapped");
        }

        InventoryItem newItem = findInventoryItemOrThrow(swapRequest.getNewItemId());
        if (newItem.getQuantity() < swapRequest.getNewQuantity()) {
            throw new InsufficientStockException(
                String.format("Insufficient stock for new item: %s. Available: %d, Requested: %d",
                    newItem.getName(), newItem.getQuantity(), swapRequest.getNewQuantity())
            );
        }

        if (!Boolean.TRUE.equals(newItem.getIsDriverIssuable())) {
            throw new InvalidOperationException(
                String.format("Item %s is not issuable to drivers", newItem.getName())
            );
        }
    }

    private DriverIssue createDriverIssue(DriverIssueRequestDTO request, Long userId) {
        // DriverIssue entity does NOT track tripId — trip info is stored on the Trip entity.
        DriverIssue issue = DriverIssue.builder()
                .issueNumber(generateIssueNumber())
                .driverId(request.getDriverId())
                .issueDate(request.getIssueDate() != null ? request.getIssueDate() : LocalDateTime.now())
                .status(STATUS_ISSUED)
                .notes(request.getNotes())
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        return driverIssueRepository.save(issue);
    }

    private DriverIssueItem processIssueItem(DriverIssue issue, DriverIssueItemRequestDTO itemReq, Long userId) {
        DriverIssueItem issueItem = DriverIssueItem.builder()
                .issue(issue)
                .itemId(itemReq.getItemId())
                .quantityIssued(itemReq.getQuantity())
                .quantityReturned(BigDecimal.ZERO)
                .conditionIssued(itemReq.getCondition())
                .notes(itemReq.getNotes())
                .isSwap(false)
                .build();

        driverIssueItemRepository.save(issueItem);

        InventoryItem item = findInventoryItemOrThrow(itemReq.getItemId());
        item.setQuantity(item.getQuantity() - itemReq.getQuantity().intValue());
        inventoryItemRepository.save(item);

        StockMovement movement = StockMovement.builder()
                .itemId(itemReq.getItemId())
                .quantity(itemReq.getQuantity().intValue())
                .movementType("OUT")
                .reason("Driver Issue")
                .notes(String.format("Issued to driver: %d", issue.getDriverId()))
                .referenceNumber(issue.getIssueNumber())
                .performedBy(String.valueOf(userId))
                .tripId(null)
                .referenceType("DRIVER_ISSUE")
                .requiresApproval(false)
                .approvalStatus("APPROVED")
                .build();

        stockMovementRepository.save(movement);

        return issueItem;
    }

    private void processReturnItem(DriverIssueItem issueItem, ReturnItemRequestDTO returnReq,
                                   DriverIssue issue, Long userId) {
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

        issueItem.setQuantityReturned(issueItem.getQuantityReturned().add(returnReq.getQuantity()));
        issueItem.setConditionReturned(returnReq.getCondition());
        issueItem.setUpdatedAt(LocalDateTime.now());
        driverIssueItemRepository.save(issueItem);

        InventoryItem item = findInventoryItemOrThrow(returnReq.getItemId());
        item.setQuantity(item.getQuantity() + returnReq.getQuantity().intValue());
        inventoryItemRepository.save(item);

        StockMovement movement = StockMovement.builder()
                .itemId(returnReq.getItemId())
                .quantity(returnReq.getQuantity().intValue())
                .movementType("IN")
                .reason("Driver Return")
                .notes(String.format("Returned from driver: %d, Condition: %s",
                    issue.getDriverId(), returnReq.getCondition()))
                .referenceNumber(issue.getIssueNumber())
                .performedBy(String.valueOf(userId))
                .referenceType("DRIVER_RETURN")
                .requiresApproval(false)
                .approvalStatus("APPROVED")
                .build();

        stockMovementRepository.save(movement);
    }

    private DriverIssueResponseDTO executeSwap(DriverIssue oldIssue, DriverIssueItem oldItem,
                                               SwapItemRequestDTO swapRequest, Long userId) {
        BigDecimal returnQuantity = swapRequest.getReturnQuantity() != null ?
                swapRequest.getReturnQuantity() : oldItem.getQuantityIssued();

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
        driverIssueItemRepository.save(oldItem);

        InventoryItem inventoryItem = findInventoryItemOrThrow(swapRequest.getOldItemId());
        inventoryItem.setHoldCode(swapRequest.getDamagedCondition());
        inventoryItem.setHoldReason(swapRequest.getDamageNotes());
        inventoryItem.setHoldDate(LocalDateTime.now());
        inventoryItem.setHeldBy(String.valueOf(userId));
        inventoryItem.setQuantity(inventoryItem.getQuantity() + returnQuantity.intValue());
        inventoryItemRepository.save(inventoryItem);

        StockMovement returnMovement = StockMovement.builder()
                .itemId(swapRequest.getOldItemId())
                .quantity(returnQuantity.intValue())
                .movementType("IN")
                .reason("Driver Swap Return - " + swapRequest.getDamagedCondition())
                .notes(String.format("Damaged item returned from driver. Hold code: %s, Notes: %s",
                    swapRequest.getDamagedCondition(),
                    swapRequest.getDamageNotes() != null ? swapRequest.getDamageNotes() : "N/A"))
                .referenceNumber(oldIssue.getIssueNumber())
                .performedBy(String.valueOf(userId))
                .referenceType("DRIVER_SWAP_RETURN")
                .requiresApproval(false)
                .approvalStatus("APPROVED")
                .build();
        stockMovementRepository.save(returnMovement);

        DriverIssueRequestDTO newIssueRequest = buildSwapIssueRequest(oldIssue, swapRequest);
        DriverIssueResponseDTO newIssue = issueItemsToDriver(newIssueRequest, userId);

        oldItem.setSwapIssueId(newIssue.getId());
        driverIssueItemRepository.save(oldItem);

        log.info("✅ Driver swap completed: Old issue {} returned, New issue {} created",
            oldIssue.getId(), newIssue.getId());

        return newIssue;
    }

    private DriverIssueRequestDTO buildSwapIssueRequest(DriverIssue oldIssue, SwapItemRequestDTO swapRequest) {
        DriverIssueRequestDTO request = new DriverIssueRequestDTO();
        request.setDriverId(oldIssue.getDriverId());
        // NOTE: DriverIssueRequestDTO still has a tripId field, but the DriverIssue
        // entity does not track it — so we leave it null here.
        request.setIssueDate(LocalDateTime.now());
        request.setNotes(String.format("SWAP: Replacing damaged item. Original Issue: %s, Damage: %s",
            oldIssue.getIssueNumber(), swapRequest.getDamagedCondition()));

        DriverIssueItemRequestDTO itemRequest = new DriverIssueItemRequestDTO();
        itemRequest.setItemId(swapRequest.getNewItemId());
        itemRequest.setQuantity(BigDecimal.valueOf(swapRequest.getNewQuantity()));
        itemRequest.setCondition("NEW");
        itemRequest.setNotes(String.format("Swap replacement for %s", swapRequest.getDamagedCondition()));

        request.setItems(List.of(itemRequest));
        return request;
    }

    private void validateReturnItems(List<ReturnItemRequestDTO> returns,
                                     Map<Long, DriverIssueItem> issueItemMap,
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
                    String.format("Item %d is not part of driver issue %d. Valid items: %s",
                        returnReq.getItemId(), issueId, validItemIds)
                );
            }
        }
    }

    private void updateIssueStatus(DriverIssue issue) {
        List<DriverIssueItem> items = driverIssueItemRepository.findByIssueId(issue.getId());

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
        driverIssueRepository.save(issue);
    }

    private String generateIssueNumber() {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5);
        return ISSUE_NUMBER_PREFIX + timestamp;
    }

    private DriverIssue getIssueOrThrow(Long issueId) {
        return driverIssueRepository.findById(issueId)
                .orElseThrow(() -> new EntityNotFoundException("Driver issue not found: " + issueId));
    }

    private DriverIssueItem getIssueItemOrThrow(Long issueId, Long itemId) {
        return driverIssueItemRepository
                .findByIssueIdAndItemId(issueId, itemId)
                .orElseThrow(() -> new EntityNotFoundException(
                    String.format("Item %d not found in driver issue %d", itemId, issueId)
                ));
    }

    private InventoryItem findInventoryItemOrThrow(Long itemId) {
        return inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory item not found: " + itemId));
    }

    private DriverIssueResponseDTO mapToResponseDTO(DriverIssue issue) {
        if (issue == null) {
            return null;
        }

        List<DriverIssueItem> items = driverIssueItemRepository.findByIssueId(issue.getId());

        List<DriverIssueItemResponseDTO> itemDTOs = items.stream()
                .map(this::mapItemToResponseDTO)
                .collect(Collectors.toList());

        return DriverIssueResponseDTO.builder()
                .id(issue.getId())
                .issueNumber(issue.getIssueNumber())
                .driverId(issue.getDriverId())
                .issueDate(issue.getIssueDate())
                .status(issue.getStatus())
                .notes(issue.getNotes())
                .items(itemDTOs)
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }

    private DriverIssueItemResponseDTO mapItemToResponseDTO(DriverIssueItem item) {
        InventoryItem inventoryItem = inventoryItemRepository.findById(item.getItemId()).orElse(null);

        return DriverIssueItemResponseDTO.builder()
                .id(item.getId())
                .itemId(item.getItemId())
                .itemName(inventoryItem != null ? inventoryItem.getName() : "Unknown")
                .itemCategory(inventoryItem != null ? inventoryItem.getCategory() : null)
                .quantityIssued(item.getQuantityIssued())
                .quantityReturned(item.getQuantityReturned())
                .quantityOutstanding(item.getQuantityIssued().subtract(item.getQuantityReturned()))
                .conditionIssued(item.getConditionIssued())
                .conditionReturned(item.getConditionReturned())
                .notes(item.getNotes())
                .build();
    }
}
