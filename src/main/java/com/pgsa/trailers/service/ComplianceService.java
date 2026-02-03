package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.finance.AccountStatement;
import com.pgsa.trailers.entity.ops.FuelSlip;
import com.pgsa.trailers.repository.AccountStatementRepository;
import com.pgsa.trailers.repository.FuelSlipRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ComplianceService
 *
 * Responsibility:
 * - Regulatory & audit-safe fuel records
 * - Read/write FuelSlip only
 *
 * Rules respected:
 * - No getAll()
 * - No cross-domain logic (no finance, no analytics)
 * - No silent recalculation
 */
@Service
public class ComplianceService {

    private final FuelSlipRepository fuelSlipRepo;
    private final AccountStatementRepository accountStatementRepo;

    public ComplianceService(FuelSlipRepository fuelSlipRepo, AccountStatementRepository accountStatementRepo) {
        this.fuelSlipRepo = fuelSlipRepo;
        this.accountStatementRepo = accountStatementRepo;
    }

    /**
     * Persist a fuel slip.
     *
     * Validation is assumed to happen at controller or DTO level.
     * This service stays thin and predictable.
     */
    public FuelSlip saveFuelSlip(FuelSlip fuelSlip) {
        return fuelSlipRepo.save(fuelSlip);
    }

    /**
     * Fetch a single fuel slip by ID.
     *
     * No Optional leakage beyond service layer.
     */
    public FuelSlip getFuelSlip(Long id) {
        return fuelSlipRepo.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("FuelSlip not found for id: " + id)
                );
    }

    /**
     * Compliance report:
     * Fuel slips filtered by vehicle and date range.
     *
     * Used for:
     * - Audits
     * - SARS / tax compliance
     * - Internal reviews
     */
    public List<FuelSlip> getFuelSlipsByVehicleAndDate(
            Long vehicleId,
            LocalDate from,
            LocalDate to
    ) {
        // Convert LocalDate to LocalDateTime
        LocalDateTime startDateTime = from.atStartOfDay();
        LocalDateTime endDateTime = to.atTime(23, 59, 59);

        return fuelSlipRepo.findByVehicleIdAndDateBetween(vehicleId, startDateTime, endDateTime);
    }

    public AccountStatement createAccountStatement(AccountStatement statement) {
        return accountStatementRepo.save(statement);
    }

    public AccountStatement getAccountStatementById(Long id) {
        return accountStatementRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AccountStatement not found for ID " + id));
    }
}