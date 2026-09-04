package com.pgsa.trailers.controller.inventory;

import com.pgsa.trailers.dto.ReturnItemRequestDTO;
import com.pgsa.trailers.dto.VehicleIssueRequestDTO;
import com.pgsa.trailers.dto.VehicleIssueResponseDTO;
import com.pgsa.trailers.entity.inventory.VehicleIssue;
import com.pgsa.trailers.entity.security.AppUser;
import com.pgsa.trailers.repository.AppUserRepository;
import com.pgsa.trailers.repository.VehicleIssueRepository;
import com.pgsa.trailers.service.inventory.VehicleIssueService;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.pgsa.trailers.dto.SwapItemRequestDTO;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/vehicle-issues")
@RequiredArgsConstructor
@Slf4j
public class VehicleIssueController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VehicleIssueController.class);

    private final VehicleIssueService vehicleIssueService;
    private final AppUserRepository appUserRepository;
    private final VehicleIssueRepository vehicleIssueRepository;

    /**
     * Get all vehicle issues
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER', 'DRIVER')")
    @GetMapping
    public ResponseEntity<List<VehicleIssueResponseDTO>> getAllVehicleIssues() {
        log.info("📋 Fetching all vehicle issues");
        try {
            List<VehicleIssueResponseDTO> issues = vehicleIssueService.getAllVehicleIssues();
            log.info("📋 Returning {} vehicle issues", issues.size());
            return ResponseEntity.ok(issues);
        } catch (Exception e) {
            log.error("❌ Error fetching vehicle issues: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
 * Swap an item - return damaged and issue replacement for vehicle
 */
@PostMapping("/{issueId}/swap")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
public ResponseEntity<VehicleIssueResponseDTO> swapItem(
        @PathVariable Long issueId,
        @RequestBody @Valid SwapItemRequestDTO swapRequest,
        Authentication authentication) {
    Long userId = getUserId(authentication);
    log.info("🔄 Swapping vehicle item from issue: {}", issueId);
    VehicleIssueResponseDTO response = vehicleIssueService.swapItem(issueId, swapRequest, userId);
    return ResponseEntity.ok(response);
}

    @GetMapping("/stock-on-hand")
public ResponseEntity<List<StockOnHandDTO>> getStockOnHand(@RequestBody(required = false) StockOnHandFilterDTO filter) {
    log.info("📊 Fetching stock on hand");
    if (filter == null) {
        filter = new StockOnHandFilterDTO();
    }
    List<StockOnHandDTO> results = vehicleIssueService.getStockOnHand(filter);
    return ResponseEntity.ok(results);
}
    
    /**
     * Create a new vehicle issue (issue items to vehicle)
     */
    @PostMapping
    public ResponseEntity<VehicleIssueResponseDTO> issueItemsToVehicle(
            @RequestBody @Valid VehicleIssueRequestDTO request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        log.info("🚗 Creating vehicle issue for vehicle: {}", request.getVehicleId());
        VehicleIssueResponseDTO response = vehicleIssueService.issueItemsToVehicle(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Return items from vehicle
     */
    @PostMapping("/{issueId}/return")
    public ResponseEntity<VehicleIssueResponseDTO> returnItemsFromVehicle(
            @PathVariable Long issueId,
            @RequestBody @Valid List<ReturnItemRequestDTO> returns,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        log.info("🔄 Returning items from issue: {}", issueId);
        VehicleIssueResponseDTO response = vehicleIssueService.returnItemsFromVehicle(issueId, returns, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get issues by vehicle ID
     */
    @GetMapping("/vehicle/{vehicleId}")
    public ResponseEntity<List<VehicleIssueResponseDTO>> getIssuesByVehicle(@PathVariable Long vehicleId) {
        log.info("🚗 Fetching issues for vehicle: {}", vehicleId);
        return ResponseEntity.ok(vehicleIssueService.getIssuesByVehicle(vehicleId));
    }

    /**
     * Get issues by driver ID
     */
    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<VehicleIssueResponseDTO>> getIssuesByDriver(@PathVariable Long driverId) {
        log.info("👤 Fetching issues for driver: {}", driverId);
        return ResponseEntity.ok(vehicleIssueService.getIssuesByDriver(driverId));
    }

    /**
     * Get issue by ID
     */
    @GetMapping("/{issueId}")
    public ResponseEntity<VehicleIssueResponseDTO> getIssueById(@PathVariable Long issueId) {
        log.info("📋 Fetching vehicle issue: {}", issueId);
        return ResponseEntity.ok(vehicleIssueService.getIssueById(issueId));
    }

    /**
     * Debug endpoint - Get all issues as entities
     */
    @GetMapping("/debug/all")
    public ResponseEntity<?> debugGetAllIssues() {
        log.info("🐛 Debug: Getting all vehicle issues from repository");
        try {
            List<VehicleIssue> issues = vehicleIssueRepository.findAll();
            log.info("🐛 Found {} issues", issues.size());
            return ResponseEntity.ok(issues);
        } catch (Exception e) {
            log.error("❌ Debug error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Debug endpoint - Get issue count
     */
    @GetMapping("/debug/count")
    public ResponseEntity<Map<String, Object>> getIssueCount() {
        log.info("🐛 Getting issue count");
        try {
            long count = vehicleIssueRepository.count();
            List<VehicleIssue> issues = vehicleIssueRepository.findAll();
            Map<String, Object> response = Map.of(
                "totalIssues", count,
                "issues", issues
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Debug error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private Long getUserId(Authentication authentication) {
        String email = authentication.getName();
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return user.getId();
    }
}
