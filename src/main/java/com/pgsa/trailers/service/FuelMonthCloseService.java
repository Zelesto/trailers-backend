package com.pgsa.trailers.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgsa.trailers.dto.FuelSlipDTO;
import com.pgsa.trailers.entity.ResourceNotFoundException;
import com.pgsa.trailers.entity.BusinessException;
import com.pgsa.trailers.entity.finance.*;
import com.pgsa.trailers.entity.ops.FuelSlip;
import com.pgsa.trailers.repository.AccountStatementRepository;
import com.pgsa.trailers.repository.FuelSlipRepository;
import com.pgsa.trailers.repository.ReconciliationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FuelMonthCloseService {

    // Repositories
    private final FuelSlipRepository fuelSlipRepository;
    private final AccountStatementRepository accountStatementRepository;
    private final ReconciliationRepository reconciliationRepository;

    // Utilities
    private final ObjectMapper objectMapper;

    // ========== CONSTANTS ==========
    private static final String SYSTEM_USER = "SYSTEM";
    private static final String AUDIT_TRAIL_ENTRIES_KEY = "entries";
    private static final String AUDIT_TRAIL_CREATED_AT_KEY = "createdAt";
    private static final String AUDIT_TRAIL_CREATED_BY_KEY = "createdBy";

    // ========== FUEL SLIP OPERATIONS ==========

    /**
     * Create a new fuel slip
     */
    @Transactional
    public FuelSlipDTO createFuelSlip(FuelSlip fuelSlip) {
        log.info("Creating fuel slip for vehicle: {}",
                extractVehicleRegistration(fuelSlip));

        validateFuelSlip(fuelSlip);
        ensureValidAuditTrail(fuelSlip);

        FuelSlip savedSlip = fuelSlipRepository.save(fuelSlip);
        log.info("Created fuel slip with ID: {}", savedSlip.getId());

        return FuelSlipDTO.fromEntity(savedSlip);
    }

    /**
     * Get fuel slip by ID
     */
    public FuelSlipDTO getFuelSlipById(Long id) {
        log.debug("Fetching fuel slip by ID: {}", id);

        FuelSlip slip = findFuelSlipById(id);
        log.debug("Found fuel slip: ID={}, Driver={}, Vehicle={}",
                slip.getId(),
                extractDriverName(slip),
                extractVehicleRegistration(slip));

        return FuelSlipDTO.fromEntity(slip);
    }

    /**
     * Finalize a fuel slip
     */
    @Transactional
    public void finalizeFuelSlip(Long id) {
        log.info("Finalizing fuel slip ID: {}", id);

        FuelSlip slip = findFuelSlipById(id);
        validateNotFinalized(slip, "finalize");

        slip.setFinalized(true);
        slip.setLastStatusUpdate(LocalDateTime.now());
        addAuditEntry(slip, "Finalized", SYSTEM_USER);

        fuelSlipRepository.save(slip);
        log.info("Fuel slip ID: {} has been finalized", id);
    }

    /**
     * Get all fuel slips
     */
    public List<FuelSlipDTO> getAllFuelSlips() {
        log.debug("Fetching all fuel slips");
        List<FuelSlipDTO> slips = fuelSlipRepository.findAll().stream()
                .map(FuelSlipDTO::fromEntity)
                .collect(Collectors.toList());
        log.debug("Found {} fuel slips", slips.size());
        return slips;
    }

    /**
     * Get fuel slips by driver
     */
    public List<FuelSlipDTO> getFuelSlipsByDriver(Long driverId) {
        log.debug("Fetching fuel slips for driver ID: {}", driverId);
        List<FuelSlipDTO> slips = fuelSlipRepository.findByDriverId(driverId).stream()
                .map(FuelSlipDTO::fromEntity)
                .collect(Collectors.toList());
        log.debug("Found {} fuel slips for driver ID: {}", slips.size(), driverId);
        return slips;
    }

    /**
     * Get fuel slips by vehicle
     */
    public List<FuelSlipDTO> getFuelSlipsByVehicle(Long vehicleId) {
        log.debug("Fetching fuel slips for vehicle ID: {}", vehicleId);
        List<FuelSlipDTO> slips = fuelSlipRepository.findByVehicleId(vehicleId).stream()
                .map(FuelSlipDTO::fromEntity)
                .collect(Collectors.toList());
        log.debug("Found {} fuel slips for vehicle ID: {}", slips.size(), vehicleId);
        return slips;
    }

    /**
     * Get fuel slips by driver and vehicle
     */
    public List<FuelSlipDTO> getFuelSlipsByDriverAndVehicle(Long driverId, Long vehicleId) {
        log.debug("Fetching fuel slips for driver ID: {} and vehicle ID: {}", driverId, vehicleId);
        List<FuelSlipDTO> slips = fuelSlipRepository.findByDriverIdAndVehicleId(driverId, vehicleId).stream()
                .map(FuelSlipDTO::fromEntity)
                .collect(Collectors.toList());
        log.debug("Found {} fuel slips for driver ID: {} and vehicle ID: {}", slips.size(), driverId, vehicleId);
        return slips;
    }

    /**
     * Get fuel slips with optional filters
     */
    public List<FuelSlipDTO> getFuelSlipsByFilters(Long driverId, Long vehicleId) {
        log.debug("Fetching fuel slips with filters - driverId: {}, vehicleId: {}", driverId, vehicleId);

        if (driverId != null && vehicleId != null) {
            return getFuelSlipsByDriverAndVehicle(driverId, vehicleId);
        } else if (driverId != null) {
            return getFuelSlipsByDriver(driverId);
        } else if (vehicleId != null) {
            return getFuelSlipsByVehicle(vehicleId);
        } else {
            return getAllFuelSlips();
        }
    }

    /**
     * Update a fuel slip
     */
    @Transactional
    public FuelSlipDTO updateFuelSlip(Long id, FuelSlip updatedSlip) {
        log.info("Updating fuel slip ID: {}", id);

        FuelSlip slip = findFuelSlipById(id);
        validateNotFinalized(slip, "update");

        updateFuelSlipFields(slip, updatedSlip);
        addAuditEntry(slip, "Updated", SYSTEM_USER);

        FuelSlip saved = fuelSlipRepository.save(slip);
        log.info("Successfully updated fuel slip ID: {}", id);

        return FuelSlipDTO.fromEntity(saved);
    }

    /**
     * Get fuel slips for a specific period
     */
    public List<FuelSlipDTO> getFuelSlipsForPeriod(LocalDate start, LocalDate end) {
        log.debug("Fetching fuel slips for period: {} to {}", start, end);

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        List<FuelSlip> slips = fuelSlipRepository.findByTransactionDateBetween(startDateTime, endDateTime);
        List<FuelSlipDTO> slipDTOs = slips.stream()
                .map(FuelSlipDTO::fromEntity)
                .collect(Collectors.toList());

        log.debug("Found {} fuel slips for period {} to {}", slipDTOs.size(), start, end);
        return slipDTOs;
    }

    /**
     * Delete a fuel slip
     */
    @Transactional
    public void deleteFuelSlip(Long id) {
        log.info("Deleting fuel slip ID: {}", id);

        FuelSlip slip = findFuelSlipById(id);
        validateNotFinalized(slip, "delete");

        fuelSlipRepository.delete(slip);
        log.info("Successfully deleted fuel slip ID: {}", id);
    }

    // ========== MONTH CLOSE OPERATIONS ==========

    /**
     * Close fuel month for an account
     */
    @Transactional
    public AccountStatement closeFuelMonth(
            Account account,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal openingBalance,
            BigDecimal paymentsTotal,
            String performedBy
    ) {
        validateMonthCloseParameters(account, periodStart, periodEnd);

        LocalDateTime from = periodStart.atStartOfDay();
        LocalDateTime to = periodEnd.atTime(23, 59, 59);

        // Check for duplicate statements
        checkForDuplicateStatement(account.getId(), periodStart, periodEnd);

        // Get and process unfinalized slips
        List<FuelSlip> slips = getUnfinalizedFuelSlipsForPeriod(account.getId(), from, to);
        BigDecimal slipsTotal = calculateSlipsTotal(slips);

        // Finalize slips
        finalizeFuelSlips(slips);

        // Create account statement
        AccountStatement statement = createAccountStatement(
                account, periodStart, periodEnd, openingBalance,
                paymentsTotal, slipsTotal, performedBy
        );

        // Create reconciliation snapshot
        createReconciliation(account, from, to, slipsTotal, paymentsTotal, performedBy);

        log.info("Month close completed for account {}: {} slips, total {}, variance {}",
                account.getName(), slips.size(), slipsTotal,
                paymentsTotal.subtract(slipsTotal));

        return statement;
    }

    // ========== HELPER METHODS ==========

    /**
     * Find fuel slip by ID or throw exception
     */
    private FuelSlip findFuelSlipById(Long id) {
        return fuelSlipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FuelSlip not found for ID " + id));
    }

    /**
     * Validate fuel slip before creation
     */
    private void validateFuelSlip(FuelSlip fuelSlip) {
        if (fuelSlip.getVehicle() == null) {
            throw new BusinessException("Vehicle is required for fuel slip");
        }
        if (fuelSlip.getQuantity() == null || fuelSlip.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Valid quantity is required");
        }
        if (fuelSlip.getUnitPrice() == null || fuelSlip.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Valid unit price is required");
        }
    }

    /**
     * Validate month close parameters
     */
    private void validateMonthCloseParameters(Account account, LocalDate periodStart, LocalDate periodEnd) {
        if (account == null) {
            throw new BusinessException("Account is required");
        }
        if (periodStart.isAfter(periodEnd)) {
            throw new BusinessException("Period start cannot be after period end");
        }
    }

    /**
     * Check for duplicate account statement
     */
    private void checkForDuplicateStatement(Long accountId, LocalDate periodStart, LocalDate periodEnd) {
        boolean alreadyClosed = accountStatementRepository.existsForAccountAndPeriod(
                accountId, periodStart, periodEnd
        );

        if (alreadyClosed) {
            throw new BusinessException(
                    String.format("Account statement already exists for period %s to %s",
                            periodStart, periodEnd)
            );
        }
    }

    /**
     * Get unfinalized fuel slips for a period
     */
    private List<FuelSlip> getUnfinalizedFuelSlipsForPeriod(Long accountId, LocalDateTime from, LocalDateTime to) {
        return fuelSlipRepository.findFuelSlipsForAccountWithinDateRangeNotFinalized(accountId, from, to);
    }

    /**
     * Calculate total amount for fuel slips
     */
    private BigDecimal calculateSlipsTotal(List<FuelSlip> slips) {
        return slips.stream()
                .map(FuelSlip::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Finalize a list of fuel slips
     */
    private void finalizeFuelSlips(List<FuelSlip> slips) {
        slips.forEach(slip -> {
            slip.setFinalized(true);
            slip.setLastStatusUpdate(LocalDateTime.now());
            addAuditEntry(slip, "Month Close Finalized", SYSTEM_USER);
        });
        fuelSlipRepository.saveAll(slips);
    }

    /**
     * Create account statement
     */
    public AccountStatement createAccountStatement(
            Account account,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal openingBalance,
            BigDecimal paymentsTotal,
            BigDecimal slipsTotal,
            String performedBy
    ) {
        BigDecimal closingBalance = openingBalance.add(paymentsTotal).subtract(slipsTotal);

        // Use manual builder since entity might not have @Builder annotation
        AccountStatement statement = new AccountStatement();
        statement.setAccount(account);
        statement.setPeriodStart(periodStart);
        statement.setPeriodEnd(periodEnd);
        statement.setStatementDate(LocalDateTime.now());
        statement.setOpeningBalance(openingBalance);
        statement.setClosingBalance(closingBalance);
        statement.setTotalDebits(slipsTotal);
        statement.setTotalCredits(paymentsTotal);
        statement.setCreatedBy(performedBy);

        return accountStatementRepository.save(statement);
    }

    /**
     * Create reconciliation snapshot
     */
    private void createReconciliation(
            Account account,
            LocalDateTime from,
            LocalDateTime to,
            BigDecimal slipsTotal,
            BigDecimal paymentsTotal,
            String performedBy
    ) {
        // Use manual builder since entity might not have @Builder annotation
        Reconciliation reconciliation = new Reconciliation();
        reconciliation.setAccountName(account.getName());
        reconciliation.setSlipsTotal(slipsTotal);
        reconciliation.setPaymentsTotal(paymentsTotal);
        reconciliation.setVariance(paymentsTotal.subtract(slipsTotal));
        reconciliation.setFrom(from);
        reconciliation.setTo(to);
        reconciliation.setCreatedBy(performedBy);

        reconciliationRepository.save(reconciliation);
    }

    /**
     * Update fuel slip fields
     */
    private void updateFuelSlipFields(FuelSlip slip, FuelSlip updatedSlip) {
        slip.setSlipNumber(updatedSlip.getSlipNumber());
        slip.setVehicle(updatedSlip.getVehicle());
        slip.setDriver(updatedSlip.getDriver());
        slip.setFuelSource(updatedSlip.getFuelSource());
        slip.setTransactionDate(updatedSlip.getTransactionDate());
        slip.setQuantity(updatedSlip.getQuantity());
        slip.setUnitPrice(updatedSlip.getUnitPrice());
        slip.setTotalAmount(updatedSlip.getTotalAmount());
        slip.setOdometerReading(updatedSlip.getOdometerReading());
        slip.setLocation(updatedSlip.getLocation());
        slip.setStationName(updatedSlip.getStationName());
        slip.setPumpNumber(updatedSlip.getPumpNumber());
        slip.setNotes(updatedSlip.getNotes());
        slip.setLoadId(updatedSlip.getLoadId());
        slip.setTripId(updatedSlip.getTripId());
        slip.setFuelType(updatedSlip.getFuelType());
        slip.setPaymentMethod(updatedSlip.getPaymentMethod());
        slip.setReceiptNumber(updatedSlip.getReceiptNumber());
        slip.setVerifiedBy(updatedSlip.getVerifiedBy());
        slip.setVerificationDate(updatedSlip.getVerificationDate());
        slip.setIncidentFlag(updatedSlip.getIncidentFlag());
        slip.setLastStatusUpdate(LocalDateTime.now());
        slip.setAccountStatement(updatedSlip.getAccountStatement());
    }

    /**
     * Validate slip is not finalized
     */
    private void validateNotFinalized(FuelSlip slip, String operation) {
        if (Boolean.TRUE.equals(slip.getFinalized())) {
            throw new BusinessException(
                    String.format("Cannot %s a finalized fuel slip", operation)
            );
        }
    }

    /**
     * Extract vehicle registration (safe)
     */
    private String extractVehicleRegistration(FuelSlip slip) {
        return slip.getVehicle() != null ? slip.getVehicle().getRegistrationNumber() : "Unknown";
    }

    /**
     * Extract driver name (safe)
     */
    private String extractDriverName(FuelSlip slip) {
        return slip.getDriver() != null ? slip.getDriver().getFullName() : "N/A";
    }

    // ========== AUDIT TRAIL METHODS ==========

    /**
     * Ensure valid audit trail structure
     */
    private void ensureValidAuditTrail(FuelSlip fuelSlip) {
        Object currentAuditTrail = fuelSlip.getAuditTrail();

        if (currentAuditTrail == null || (currentAuditTrail instanceof Map && ((Map<?, ?>) currentAuditTrail).isEmpty())) {
            fuelSlip.setAuditTrail(createInitialAuditTrail());
        } else if (currentAuditTrail instanceof String) {
            fuelSlip.setAuditTrail(convertStringToAuditTrail((String) currentAuditTrail));
        }
        // If it's already a Map, do nothing
    }

    /**
     * Create initial audit trail structure
     */
    private Map<String, Object> createInitialAuditTrail() {
        Map<String, Object> auditTrail = new HashMap<>();
        auditTrail.put(AUDIT_TRAIL_ENTRIES_KEY, new ArrayList<Map<String, Object>>());
        auditTrail.put(AUDIT_TRAIL_CREATED_AT_KEY, LocalDateTime.now().toString());
        auditTrail.put(AUDIT_TRAIL_CREATED_BY_KEY, SYSTEM_USER);
        return auditTrail;
    }

    /**
     * Convert string to audit trail map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertStringToAuditTrail(String auditTrailString) {
        try {
            // Try to parse as Map
            return objectMapper.readValue(auditTrailString, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            // If parsing fails, create new audit trail with legacy content as note
            Map<String, Object> newAuditTrail = createInitialAuditTrail();
            newAuditTrail.put("legacyContent", auditTrailString);
            return newAuditTrail;
        }
    }

    /**
     * Add audit entry to fuel slip
     */
    @SuppressWarnings("unchecked")
    private void addAuditEntry(FuelSlip slip, String action, String performedBy) {
        try {
            Map<String, Object> auditTrail = slip.getAuditTrail();
            if (auditTrail == null) {
                auditTrail = createInitialAuditTrail();
                slip.setAuditTrail(auditTrail);
            }

            List<Map<String, Object>> entries = (List<Map<String, Object>>) auditTrail.getOrDefault(
                    AUDIT_TRAIL_ENTRIES_KEY, new ArrayList<>()
            );

            Map<String, Object> newEntry = createAuditEntry(action, performedBy);
            entries.add(newEntry);

            auditTrail.put(AUDIT_TRAIL_ENTRIES_KEY, entries);
            auditTrail.put("lastUpdated", LocalDateTime.now().toString());

        } catch (Exception e) {
            log.warn("Could not add audit entry for slip {}: {}", slip.getId(), e.getMessage());
            // Fallback: add simple audit trail
            slip.setAuditTrail(createInitialAuditTrail());
        }
    }

    /**
     * Create an audit entry
     */
    private Map<String, Object> createAuditEntry(String action, String performedBy) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("timestamp", LocalDateTime.now().toString());
        entry.put("action", action);
        entry.put("performedBy", performedBy);
        entry.put("details", String.format("%s performed by %s", action, performedBy));
        return entry;
    }
}

