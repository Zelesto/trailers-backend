package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.*;
import com.pgsa.trailers.entity.security.AppUser;
import com.pgsa.trailers.repository.AppUserRepository;
import com.pgsa.trailers.service.DriverIssueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/driver-issues")
@RequiredArgsConstructor
@Slf4j
public class DriverIssueController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DriverIssueController.class);

    private final DriverIssueService driverIssueService;
    private final AppUserRepository appUserRepository;

    /**
     * Get all driver issues
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER', 'DRIVER')")
    public ResponseEntity<List<DriverIssueResponseDTO>> getAllDriverIssues() {
        log.info("📋 Fetching all driver issues");
        try {
            List<DriverIssueResponseDTO> issues = driverIssueService.getAllDriverIssues();
            log.info("📋 Returning {} driver issues", issues.size());
            return ResponseEntity.ok(issues);
        } catch (Exception e) {
            log.error("❌ Error fetching driver issues: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
 * Swap an item - return damaged and issue replacement for driver
 */
@PostMapping("/{issueId}/swap")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
public ResponseEntity<DriverIssueResponseDTO> swapDriverItem(
        @PathVariable Long issueId,
        @RequestBody @Valid SwapItemRequestDTO swapRequest,
        Authentication authentication) {
    Long userId = getUserId(authentication);
    log.info("🔄 Swapping driver item from issue: {}", issueId);
    DriverIssueResponseDTO response = driverIssueService.swapItem(issueId, swapRequest, userId);
    return ResponseEntity.ok(response);
}

    /**
     * Create a new driver issue
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
    public ResponseEntity<DriverIssueResponseDTO> issueItemsToDriver(
            @RequestBody @Valid DriverIssueRequestDTO request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        log.info("👤 Creating driver issue for driver: {}", request.getDriverId());
        DriverIssueResponseDTO response = driverIssueService.issueItemsToDriver(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Return items from driver
     */
    @PostMapping("/{issueId}/return")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
    public ResponseEntity<DriverIssueResponseDTO> returnItemsFromDriver(
            @PathVariable Long issueId,
            @RequestBody @Valid List<ReturnItemRequestDTO> returns,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        log.info("🔄 Returning items from driver issue: {}", issueId);
        DriverIssueResponseDTO response = driverIssueService.returnItemsFromDriver(issueId, returns, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get issues by driver ID
     */
    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER', 'DRIVER')")
    public ResponseEntity<List<DriverIssueResponseDTO>> getIssuesByDriver(@PathVariable Long driverId) {
        log.info("👤 Fetching issues for driver: {}", driverId);
        return ResponseEntity.ok(driverIssueService.getIssuesByDriver(driverId));
    }
    
    @PutMapping("/{issueId}")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
public ResponseEntity<DriverIssueResponseDTO> updateDriverIssue(
        @PathVariable Long issueId,
        @RequestBody @Valid DriverIssueRequestDTO request,
        Authentication authentication) {
    Long userId = getUserId(authentication);
    log.info("✏️ Updating driver issue: {}", issueId);
    DriverIssueResponseDTO response = driverIssueService.updateDriverIssue(issueId, request, userId);
    return ResponseEntity.ok(response);
}

@DeleteMapping("/{issueId}")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER')")
public ResponseEntity<Void> deleteDriverIssue(
        @PathVariable Long issueId,
        Authentication authentication) {
    Long userId = getUserId(authentication);
    log.info("🗑️ Deleting driver issue: {}", issueId);
    driverIssueService.deleteDriverIssue(issueId, userId);
    return ResponseEntity.noContent().build();
}

    /**
     * Get issue by ID
     */
    @GetMapping("/{issueId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER', 'MANAGER', 'DRIVER')")
    public ResponseEntity<DriverIssueResponseDTO> getIssueById(@PathVariable Long issueId) {
        log.info("📋 Fetching driver issue: {}", issueId);
        return ResponseEntity.ok(driverIssueService.getIssueById(issueId));
    }

    private Long getUserId(Authentication authentication) {
        String email = authentication.getName();
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return user.getId();
    }
}
