// src/main/java/com/pgsa/trailers/controller/InventoryController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.*;
import com.pgsa.trailers.dto.InventoryVarianceDTO;
import com.pgsa.trailers.dto.*;
import com.pgsa.trailers.entity.inventory.StockMovement;
import com.pgsa.trailers.repository.InventoryItemRepository;
import com.pgsa.trailers.service.StockCountService;
import com.pgsa.trailers.service.InventoryItemService;
import com.pgsa.trailers.service.InventoryLocationService;
import com.pgsa.trailers.service.StockMovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
public class InventoryController {

    private final InventoryItemService inventoryItemService;
    private final InventoryLocationService inventoryLocationService;
    private final StockMovementService stockMovementService;
    private final StockCountService stockCountService;
    private final InventoryItemRepository inventoryItemRepository; // Add this

    // =============================================
    // Inventory Items
    // =============================================

    @GetMapping("/items")
    public ResponseEntity<Page<InventoryItemResponseDTO>> getAllItems(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        log.info("Fetching all inventory items");
        return ResponseEntity.ok(inventoryItemService.getAllItems(pageable));
    }

    @GetMapping("/items/search")
    public ResponseEntity<Page<InventoryItemResponseDTO>> searchItems(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        log.info("Searching inventory items with term: {}", search);
        return ResponseEntity.ok(inventoryItemService.searchItems(search, pageable));
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<InventoryItemResponseDTO> getItemById(@PathVariable Long id) {
        log.info("Fetching inventory item with ID: {}", id);
        return ResponseEntity.ok(inventoryItemService.getItemById(id));
    }

    @GetMapping("/items/category/{category}")
    public ResponseEntity<List<InventoryItemResponseDTO>> getItemsByCategory(@PathVariable String category) {
        log.info("Fetching inventory items by category: {}", category);
        return ResponseEntity.ok(inventoryItemService.getItemsByCategory(category));
    }

    @GetMapping("/items/location/{locationId}")
    public ResponseEntity<List<InventoryItemResponseDTO>> getItemsByLocation(@PathVariable Long locationId) {
        log.info("Fetching inventory items by location ID: {}", locationId);
        return ResponseEntity.ok(inventoryItemService.getItemsByLocation(locationId));
    }

    @GetMapping("/items/consumable")
    public ResponseEntity<List<InventoryItemResponseDTO>> getConsumableItems() {
        log.info("Fetching consumable inventory items");
        return ResponseEntity.ok(inventoryItemService.getConsumableItems());
    }

    @PostMapping("/items")
    public ResponseEntity<InventoryItemResponseDTO> createItem(@Valid @RequestBody InventoryItemRequestDTO request) {
        log.info("Creating new inventory item: {}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryItemService.createItem(request));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<InventoryItemResponseDTO> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody InventoryItemRequestDTO request) {
        log.info("Updating inventory item with ID: {}", id);
        return ResponseEntity.ok(inventoryItemService.updateItem(id, request));
    }

    @PatchMapping("/items/{id}/quantity")
    public ResponseEntity<InventoryItemResponseDTO> updateQuantity(
            @PathVariable Long id,
            @RequestParam Integer quantity,
            @RequestParam(defaultValue = "SET") String operation) {
        log.info("Updating quantity for item ID: {} with operation: {} quantity: {}", id, operation, quantity);
        return ResponseEntity.ok(inventoryItemService.updateQuantity(id, quantity, operation));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        log.info("Deleting inventory item with ID: {}", id);
        inventoryItemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/items/low-stock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
    public ResponseEntity<List<InventoryItemResponseDTO>> getLowStockItems() {
        log.info("Fetching low stock items");
        List<InventoryItemResponseDTO> lowStockItems = inventoryItemRepository.findLowStockItems()
                .stream()
                .map(inventoryItemService::mapToResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lowStockItems);
    }

    @GetMapping("/items/out-of-stock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
    public ResponseEntity<List<InventoryItemResponseDTO>> getOutOfStockItems() {
        log.info("Fetching out of stock items");
        List<InventoryItemResponseDTO> outOfStockItems = inventoryItemRepository.findOutOfStockItems()
                .stream()
                .map(inventoryItemService::mapToResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(outOfStockItems);
    }
    
    // =============================================
    // Inventory Locations
    // =============================================

    @GetMapping("/locations")
    public ResponseEntity<List<InventoryLocationResponseDTO>> getAllLocations() {
        log.info("Fetching all inventory locations");
        return ResponseEntity.ok(inventoryLocationService.getAllLocations());
    }

    @GetMapping("/locations/{id}")
    public ResponseEntity<InventoryLocationResponseDTO> getLocationById(@PathVariable Long id) {
        log.info("Fetching inventory location with ID: {}", id);
        return ResponseEntity.ok(inventoryLocationService.getLocationById(id));
    }

    @PostMapping("/locations")
    public ResponseEntity<InventoryLocationResponseDTO> createLocation(
            @Valid @RequestBody InventoryLocationRequestDTO request) {
        log.info("Creating new inventory location: {}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryLocationService.createLocation(request));
    }

    @PutMapping("/locations/{id}")
    public ResponseEntity<InventoryLocationResponseDTO> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody InventoryLocationRequestDTO request) {
        log.info("Updating inventory location with ID: {}", id);
        return ResponseEntity.ok(inventoryLocationService.updateLocation(id, request));
    }

    @DeleteMapping("/locations/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        log.info("Deleting inventory location with ID: {}", id);
        inventoryLocationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }

    // =============================================
    // Stock Movements (Full CRUD)
    // =============================================

    @PostMapping("/recordMovement")
    public ResponseEntity<StockMovementResponseDTO> recordMovement(@Valid @RequestBody StockMovementRequestDTO request) {
        log.info("Recording stock movement: {}", request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockMovementService.recordMovement(request));
    }

    @GetMapping("/movements")
    public ResponseEntity<Page<StockMovementResponseDTO>> getAllMovements(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching all stock movements");
        return ResponseEntity.ok(stockMovementService.getAllMovements(pageable));
    }

    @GetMapping("/movements/{id}")
    public ResponseEntity<StockMovementResponseDTO> getMovementById(@PathVariable Long id) {
        log.info("Fetching stock movement with ID: {}", id);
        return ResponseEntity.ok(stockMovementService.getMovementById(id));
    }

    @GetMapping("/movements/item/{itemId}")
    public ResponseEntity<Page<StockMovementResponseDTO>> getMovementsByItem(
            @PathVariable Long itemId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching movements for item ID: {}", itemId);
        return ResponseEntity.ok(stockMovementService.getMovementsByItem(itemId, pageable));
    }

    @GetMapping("/movements/trip/{tripId}")
    public ResponseEntity<List<StockMovementResponseDTO>> getMovementsByTrip(@PathVariable Long tripId) {
        log.info("Fetching movements for trip ID: {}", tripId);
        return ResponseEntity.ok(stockMovementService.getMovementsByTrip(tripId));
    }

    @GetMapping("/movements/fuel-slip/{fuelSlipId}")
    public ResponseEntity<List<StockMovementResponseDTO>> getMovementsByFuelSlip(@PathVariable Long fuelSlipId) {
        log.info("Fetching movements for fuel slip ID: {}", fuelSlipId);
        return ResponseEntity.ok(stockMovementService.getMovementsByFuelSlip(fuelSlipId));
    }

    @GetMapping("/movements/pending")
    public ResponseEntity<List<StockMovementResponseDTO>> getPendingApprovals() {
        log.info("Fetching pending approvals");
        return ResponseEntity.ok(stockMovementService.getPendingApprovals());
    }

    @PatchMapping("/movements/{id}/approve")
    public ResponseEntity<StockMovementResponseDTO> approveMovement(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        log.info("Approving movement with ID: {}", id);
        String approvedBy = request.getOrDefault("approvedBy", "System");
        String notes = request.getOrDefault("notes", "");
        return ResponseEntity.ok(stockMovementService.approveMovement(id, approvedBy, notes));
    }

    @PatchMapping("/movements/{id}/reject")
    public ResponseEntity<StockMovementResponseDTO> rejectMovement(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        log.info("Rejecting movement with ID: {}", id);
        String rejectedBy = request.getOrDefault("rejectedBy", "System");
        String reason = request.getOrDefault("reason", "No reason provided");
        return ResponseEntity.ok(stockMovementService.rejectMovement(id, rejectedBy, reason));
    }

    @GetMapping("/movements/stats")
    public ResponseEntity<Map<String, Object>> getMovementStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("Fetching movement statistics");
        return ResponseEntity.ok(stockMovementService.getMovementStats(startDate, endDate));
    }

    // =============================================
    // Shrinkage Reports
    // =============================================

    @GetMapping("/shrinkage/{id}")
    public ResponseEntity<InventoryVarianceDTO> getShrinkage(@PathVariable Long id) {
        log.info("Getting shrinkage report for item: {}", id);
        return ResponseEntity.ok(stockCountService.getShrinkageReport(id));
    }

    // =============================================
    // Statistics
    // =============================================

    @GetMapping("/stats")
    public ResponseEntity<InventoryStatisticsDTO> getStatistics() {
        log.info("Fetching inventory statistics");
        return ResponseEntity.ok(inventoryItemService.getStatistics());
    }
}
