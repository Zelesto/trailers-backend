// src/main/java/com/pgsa/trailers/service/inventory/StockMovementService.java
package com.pgsa.trailers.service.inventory;

import com.pgsa.trailers.dto.StockMovementRequestDTO;
import com.pgsa.trailers.dto.StockMovementResponseDTO;
import com.pgsa.trailers.entity.inventory.InventoryItem;
import com.pgsa.trailers.entity.inventory.StockMovement;
import com.pgsa.trailers.entity.InsufficientStockException;
import com.pgsa.trailers.repository.InventoryItemRepository;
import com.pgsa.trailers.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final InventoryItemRepository inventoryItemRepository;

    /**
     * Record a new stock movement with validation
     */
    public StockMovementResponseDTO recordMovement(StockMovementRequestDTO request) {
        log.info("Recording stock movement for item: {}, type: {}, quantity: {}", 
                request.getItemId(), request.getMovementType(), request.getQuantity());

        // Validate item exists
        InventoryItem item = inventoryItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new RuntimeException("Inventory item not found with ID: " + request.getItemId()));

        log.info("Current stock for item {}: {} {}", item.getName(), item.getQuantity(), item.getUnitOfMeasure());

        // Validate movement type
        if (!request.getMovementType().matches("IN|OUT|ADJUSTMENT")) {
            throw new RuntimeException("Invalid movement type. Use IN, OUT, or ADJUSTMENT");
        }

        // =============================================
        // STOCK VALIDATION FOR OUT MOVEMENTS
        // =============================================
        if ("OUT".equals(request.getMovementType())) {
            int currentQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
            int requestedQuantity = request.getQuantity();
            
            // Check if there's enough stock
            if (currentQuantity < requestedQuantity) {
                throw new InsufficientStockException(
                    "Insufficient stock for item: " + item.getName() + 
                    ". Available: " + currentQuantity + 
                    ", Requested: " + requestedQuantity + 
                    ". Please reduce the quantity or add stock first.",
                    item.getId(),
                    item.getName(),
                    currentQuantity,
                    requestedQuantity
                );
            }
            
            // Check if stock will be critically low (warning)
            if (item.getMinLevel() != null) {
                int remainingAfter = currentQuantity - requestedQuantity;
                if (remainingAfter <= item.getMinLevel()) {
                    log.warn("⚠️ Stock will fall below minimum level for item: {} after this movement. " +
                             "Current: {}, Requested: {}, Remaining: {}, Min Level: {}", 
                             item.getName(), currentQuantity, requestedQuantity, remainingAfter, item.getMinLevel());
                }
                if (remainingAfter <= 0) {
                    log.warn("🔥 Item {} will be OUT OF STOCK after this movement!", item.getName());
                }
            }
        }

        // =============================================
        // Determine if approval is required
        // =============================================
        boolean requiresApproval = request.getRequiresApproval() != null ? request.getRequiresApproval() : false;
        
        // Stock OUT and ADJUSTMENT always require approval
        if ("OUT".equals(request.getMovementType()) || "ADJUSTMENT".equals(request.getMovementType())) {
            requiresApproval = true;
        }
        
        // Stock IN requires approval if no reference number provided
        if ("IN".equals(request.getMovementType()) && (request.getReferenceNumber() == null || request.getReferenceNumber().isEmpty())) {
            requiresApproval = true;
        }

        // Generate internal reference number if not provided
        String referenceNumber = request.getReferenceNumber();
        if (referenceNumber == null || referenceNumber.isEmpty()) {
            referenceNumber = generateReferenceNumber(request.getMovementType());
        }

        // =============================================
        // For OUT movements with sufficient stock, allow direct approval
        // =============================================
        // If it's an OUT movement and there's enough stock, 
        // the user can choose to approve immediately or submit for approval
        // This is controlled by the frontend

        // Create movement record with PENDING status
        StockMovement movement = StockMovement.builder()
                .itemId(request.getItemId())
                .quantity(request.getQuantity())
                .movementType(request.getMovementType())
                .reason(request.getReason())
                .notes(request.getNotes())
                .referenceNumber(referenceNumber)
                .referenceType(request.getReferenceType())
                .performedBy(request.getPerformedBy())
                .tripId(request.getTripId())
                .fuelSlipId(request.getFuelSlipId())
                .requiresApproval(requiresApproval)
                .approvalStatus("PENDING") // Always start as PENDING
                .build();

        StockMovement saved = stockMovementRepository.save(movement);
        log.info("✅ Recorded stock movement for item {}: {} {} units, Reference: {}, Approval: {}", 
                request.getItemId(), request.getMovementType(), request.getQuantity(), 
                referenceNumber, requiresApproval ? "PENDING" : "AUTO-APPROVED");

        // DO NOT update inventory quantity here - wait for approval
        // The inventory will be updated when the movement is approved

        return mapToResponseDTO(saved);
    }

    /**
     * Get all movements with pagination
     */
    @Transactional(readOnly = true)
    public Page<StockMovementResponseDTO> getAllMovements(Pageable pageable) {
        log.info("Fetching all stock movements with pagination");
        return stockMovementRepository.findAll(pageable)
                .map(this::mapToResponseDTO);
    }

    /**
     * Get movement by ID
     */
    @Transactional(readOnly = true)
    public StockMovementResponseDTO getMovementById(Long id) {
        log.info("Fetching stock movement with ID: {}", id);
        StockMovement movement = stockMovementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock movement not found with ID: " + id));
        return mapToResponseDTO(movement);
    }

    /**
     * Get movements by item with pagination
     */
    @Transactional(readOnly = true)
    public Page<StockMovementResponseDTO> getMovementsByItem(Long itemId, Pageable pageable) {
        log.info("Fetching movements for item ID: {}", itemId);
        return stockMovementRepository.findByItemId(itemId, pageable)
                .map(this::mapToResponseDTO);
    }

    /**
     * Get movements by trip
     */
    @Transactional(readOnly = true)
    public List<StockMovementResponseDTO> getMovementsByTrip(Long tripId) {
        log.info("Fetching movements for trip ID: {}", tripId);
        return stockMovementRepository.findByTripId(tripId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get movements by fuel slip
     */
    @Transactional(readOnly = true)
    public List<StockMovementResponseDTO> getMovementsByFuelSlip(Long fuelSlipId) {
        log.info("Fetching movements for fuel slip ID: {}", fuelSlipId);
        return stockMovementRepository.findByFuelSlipId(fuelSlipId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get movements by date range
     */
    @Transactional(readOnly = true)
    public List<StockMovementResponseDTO> getMovementsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching movements from {} to {}", startDate, endDate);
        return stockMovementRepository.findByDateRange(startDate, endDate)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get pending approvals
     */
    @Transactional(readOnly = true)
    public List<StockMovementResponseDTO> getPendingApprovals() {
        log.info("Fetching pending approvals");
        return stockMovementRepository.findByApprovalStatus("PENDING")
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Approve a movement - updates inventory
     */
    @Transactional
    public StockMovementResponseDTO approveMovement(Long id, String approvedBy, String notes) {
        log.info("Approving movement with ID: {} by {}", id, approvedBy);
        
        StockMovement movement = stockMovementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock movement not found with ID: " + id));
        
        if (!"PENDING".equals(movement.getApprovalStatus())) {
            throw new RuntimeException("Movement is not in pending status. Current status: " + movement.getApprovalStatus());
        }
        
        // For OUT movements, check stock again before approving
        if ("OUT".equals(movement.getMovementType())) {
            InventoryItem item = inventoryItemRepository.findById(movement.getItemId())
                    .orElseThrow(() -> new RuntimeException("Item not found"));
            
            int currentQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
            if (currentQuantity < movement.getQuantity()) {
                throw new InsufficientStockException(
                    "Cannot approve: Insufficient stock for " + item.getName() + 
                    ". Available: " + currentQuantity + 
                    ", Requested: " + movement.getQuantity() +
                    ". Stock may have been consumed while waiting for approval."
                );
            }
        }
        
        movement.setApprovalStatus("APPROVED");
        movement.setApprovedBy(approvedBy);
        movement.setApprovedAt(LocalDateTime.now());
        movement.setApprovalNotes(notes);
        
        // Update inventory quantity when approved
        updateInventoryQuantity(movement);
        
        StockMovement updated = stockMovementRepository.save(movement);
        log.info("✅ Movement {} approved by: {} - Inventory updated", id, approvedBy);
        return mapToResponseDTO(updated);
    }

    /**
     * Reject a movement
     */
    @Transactional
    public StockMovementResponseDTO rejectMovement(Long id, String rejectedBy, String reason) {
        log.info("Rejecting movement with ID: {} by {}", id, rejectedBy);
        
        StockMovement movement = stockMovementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock movement not found with ID: " + id));
        
        if (!"PENDING".equals(movement.getApprovalStatus())) {
            throw new RuntimeException("Movement is not in pending status. Current status: " + movement.getApprovalStatus());
        }
        
        movement.setApprovalStatus("REJECTED");
        movement.setRejectedBy(rejectedBy);
        movement.setRejectedAt(LocalDateTime.now());
        movement.setRejectionReason(reason);
        
        StockMovement updated = stockMovementRepository.save(movement);
        log.info("❌ Movement {} rejected by: {}, reason: {}", id, rejectedBy, reason);
        return mapToResponseDTO(updated);
    }

    /**
     * Get movement statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getMovementStats(String startDate, String endDate) {
        log.info("Fetching movement statistics");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMovements", stockMovementRepository.count());
        stats.put("pendingApprovals", stockMovementRepository.countByApprovalStatus("PENDING"));
        stats.put("approvedMovements", stockMovementRepository.countByApprovalStatus("APPROVED"));
        stats.put("rejectedMovements", stockMovementRepository.countByApprovalStatus("REJECTED"));
        
        // Get counts by movement type
        stats.put("stockIn", stockMovementRepository.countByMovementType("IN"));
        stats.put("stockOut", stockMovementRepository.countByMovementType("OUT"));
        stats.put("adjustments", stockMovementRepository.countByMovementType("ADJUSTMENT"));
        
        // Get total quantities by type
        stats.put("totalInQuantity", getTotalQuantityByType("IN"));
        stats.put("totalOutQuantity", getTotalQuantityByType("OUT"));
        
        return stats;
    }

    /**
     * Check if item has sufficient stock
     */
    @Transactional(readOnly = true)
    public boolean hasSufficientStock(Long itemId, int requestedQuantity) {
        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        int available = item.getQuantity() != null ? item.getQuantity() : 0;
        return available >= requestedQuantity;
    }

    /**
     * Get current stock level for an item
     */
    @Transactional(readOnly = true)
    public int getCurrentStock(Long itemId) {
        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        return item.getQuantity() != null ? item.getQuantity() : 0;
    }

    /**
     * Update inventory quantity based on movement
     */
    private void updateInventoryQuantity(StockMovement movement) {
        InventoryItem item = inventoryItemRepository.findById(movement.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found with ID: " + movement.getItemId()));
        
        int currentQuantity = item.getQuantity() != null ? item.getQuantity() : 0;
        int newQuantity;
        
        switch (movement.getMovementType()) {
            case "IN":
                newQuantity = currentQuantity + movement.getQuantity();
                log.info("Adding {} units to item {}. Current: {}, New: {}", 
                        movement.getQuantity(), item.getName(), currentQuantity, newQuantity);
                break;
            case "OUT":
                if (currentQuantity < movement.getQuantity()) {
                    throw new InsufficientStockException(
                        "Insufficient stock for " + item.getName() + 
                        ". Available: " + currentQuantity + 
                        ", Requested: " + movement.getQuantity()
                    );
                }
                newQuantity = currentQuantity - movement.getQuantity();
                log.info("Removing {} units from item {}. Current: {}, New: {}", 
                        movement.getQuantity(), item.getName(), currentQuantity, newQuantity);
                
                // Check if stock is now below minimum
                if (item.getMinLevel() != null && newQuantity <= item.getMinLevel()) {
                    log.warn("⚠️ Stock for item {} is now at {} (Min: {})", 
                            item.getName(), newQuantity, item.getMinLevel());
                }
                if (newQuantity <= 0) {
                    log.warn("🔥 Item {} is now OUT OF STOCK!", item.getName());
                }
                break;
            case "ADJUSTMENT":
                newQuantity = movement.getQuantity();
                log.info("Adjusting item {} quantity to: {}", item.getName(), newQuantity);
                break;
            default:
                throw new RuntimeException("Invalid movement type: " + movement.getMovementType());
        }
        
        item.setQuantity(newQuantity);
        inventoryItemRepository.save(item);
        log.info("✅ Updated quantity for item {} to: {}", item.getId(), newQuantity);
    }

    /**
     * Generate a reference number
     */
    private String generateReferenceNumber(String type) {
        String prefix = "IN".equals(type) ? "IN" : 
                        "OUT".equals(type) ? "OUT" : "ADJ";
        return prefix + "-" + System.currentTimeMillis();
    }

    /**
     * Get total quantity by movement type
     */
    private int getTotalQuantityByType(String type) {
        Integer total = stockMovementRepository.sumQuantityByMovementType(type);
        return total != null ? total : 0;
    }

    /**
     * Map entity to response DTO
     */
    private StockMovementResponseDTO mapToResponseDTO(StockMovement movement) {
        // Get item name
        String itemName = null;
        Optional<InventoryItem> optionalItem = inventoryItemRepository.findById(movement.getItemId());
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
                .referenceType(movement.getReferenceType())
                .performedBy(movement.getPerformedBy())
                .createdAt(movement.getCreatedAt())
                .tripId(movement.getTripId())
                .fuelSlipId(movement.getFuelSlipId())
                .requiresApproval(movement.getRequiresApproval())
                .approvalStatus(movement.getApprovalStatus())
                .approvedBy(movement.getApprovedBy())
                .approvedAt(movement.getApprovedAt())
                .approvalNotes(movement.getApprovalNotes())
                .rejectedBy(movement.getRejectedBy())
                .rejectedAt(movement.getRejectedAt())
                .rejectionReason(movement.getRejectionReason())
                .build();
    }
}
