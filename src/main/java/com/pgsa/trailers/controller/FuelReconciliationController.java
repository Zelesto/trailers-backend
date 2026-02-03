package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.FuelReconciliationDTO;
import com.pgsa.trailers.service.FuelReconciliationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/fuel/reconciliation")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
public class FuelReconciliationController {

    private final FuelReconciliationService fuelService;

    public FuelReconciliationController(FuelReconciliationService fuelService) {
        this.fuelService = fuelService;
    }

    /**
     * Reconcile fuel for a specific period
     * Example: /api/fuel/reconciliation?from=2026-01-01T00:00:00&to=2026-01-09T23:59:59
     */
    @GetMapping
    public List<FuelReconciliationDTO> reconcileFuel(
            @RequestParam String from,
            @RequestParam String to
    ) {
        LocalDateTime fromDate = LocalDateTime.parse(from);
        LocalDateTime toDate = LocalDateTime.parse(to);
        return fuelService.reconcileFuel(fromDate, toDate);
    }

    /**
     * Fetch reconciliation for a single account by name
     * Example: /api/fuel/reconciliation/account?from=...&to=...&accountName=FuelCo
     */
    @GetMapping("/account")
    public FuelReconciliationDTO getReconciliationByAccount(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String accountName
    ) {
        LocalDateTime fromDate = LocalDateTime.parse(from);
        LocalDateTime toDate = LocalDateTime.parse(to);
        return fuelService.getReconciliationByAccountName(fromDate, toDate, accountName);
    }
}
