// src/main/java/com/pgsa/trailers/controller/LeaveController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.LeaveRequestDTO;
import com.pgsa.trailers.entity.attendance.LeaveBalance;
import com.pgsa.trailers.entity.attendance.LeaveRequest;
import com.pgsa.trailers.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveController {

    private static final Logger log = LoggerFactory.getLogger(LeaveController.class);
    
    private final LeaveService leaveService;

    @PostMapping("/request")
    public ResponseEntity<?> requestLeave(@RequestBody LeaveRequestDTO request) {
        log.info("POST /api/leave/request - Driver: {}", request.getDriverId());
        try {
            LeaveRequest leaveRequest = leaveService.requestLeave(request);
            return ResponseEntity.ok(leaveRequest);
        } catch (RuntimeException e) {
            log.error("Error requesting leave: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error requesting leave: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to request leave");
        }
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveLeave(
            @PathVariable Long id,
            @RequestParam Long approverId) {
        log.info("PUT /api/leave/{}/approve", id);
        try {
            LeaveRequest leaveRequest = leaveService.approveLeave(id, approverId);
            return ResponseEntity.ok(leaveRequest);
        } catch (RuntimeException e) {
            log.error("Error approving leave: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error approving leave: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to approve leave");
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectLeave(
            @PathVariable Long id,
            @RequestParam String reason) {
        log.info("PUT /api/leave/{}/reject", id);
        try {
            LeaveRequest leaveRequest = leaveService.rejectLeave(id, reason);
            return ResponseEntity.ok(leaveRequest);
        } catch (RuntimeException e) {
            log.error("Error rejecting leave: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error rejecting leave: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to reject leave");
        }
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<LeaveRequest>> getLeaveRequests(@PathVariable Long driverId) {
        log.info("GET /api/leave/driver/{}", driverId);
        try {
            List<LeaveRequest> requests = leaveService.getLeaveRequestsByDriver(driverId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            log.error("Error fetching leave requests: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/driver/{driverId}/balances")
    public ResponseEntity<List<LeaveBalance>> getLeaveBalances(@PathVariable Long driverId) {
        log.info("GET /api/leave/driver/{}/balances", driverId);
        try {
            List<LeaveBalance> balances = leaveService.getLeaveBalances(driverId);
            return ResponseEntity.ok(balances);
        } catch (Exception e) {
            log.error("Error fetching leave balances: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
