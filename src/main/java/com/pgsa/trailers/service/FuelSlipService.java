package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.FuelSlipDTO;
import com.pgsa.trailers.dto.FuelSlipRequest;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.entity.finance.AccountStatement;
import com.pgsa.trailers.entity.ops.FuelSource;
import com.pgsa.trailers.entity.ops.FuelSlip;
import com.pgsa.trailers.enums.DriverStatus;
import com.pgsa.trailers.repository.FuelSlipRepository;
import com.pgsa.trailers.repository.DriverRepository;
import com.pgsa.trailers.repository.VehicleRepository;
import com.pgsa.trailers.repository.FuelSourceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FuelSlipService {

    private final FuelSlipRepository fuelSlipRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final FuelSourceRepository fuelSourceRepository;

    @Transactional
    public FuelSlipDTO createFuelSlip(FuelSlipRequest request) {
        log.info("Creating fuel slip: {}", request);

        // Validate required fields
        validateFuelSlipRequest(request);

        // Create the fuel slip entity
        FuelSlip fuelSlip = new FuelSlip();

        // ========== SET SLIP NUMBER ==========
        if (request.getSlipNumber() == null || request.getSlipNumber().trim().isEmpty()) {
            fuelSlip.setSlipNumber(generateSlipNumber());
        } else {
            fuelSlip.setSlipNumber(request.getSlipNumber().trim());
        }

        // ========== SET TRANSACTION DATE ==========
        if (request.getTransactionDate() != null) {
            fuelSlip.setTransactionDate(request.getTransactionDate());
        } else {
            fuelSlip.setTransactionDate(LocalDateTime.now());
        }

        // ========== HANDLE VEHICLE ==========
        handleVehicleAssignment(request, fuelSlip);

        // ========== HANDLE DRIVER ==========
        handleDriverAssignment(request, fuelSlip);

        // ========== HANDLE FUEL SOURCE ==========
        handleFuelSource(request, fuelSlip);

        // ========== SET FUEL DETAILS ==========
        fuelSlip.setQuantity(new BigDecimal(request.getQuantity().toString()));
        fuelSlip.setUnitPrice(new BigDecimal(request.getUnitPrice().toString()));

        // Calculate total amount
        BigDecimal quantity = new BigDecimal(request.getQuantity().toString());
        BigDecimal unitPrice = new BigDecimal(request.getUnitPrice().toString());
        BigDecimal totalAmount = quantity.multiply(unitPrice);
        fuelSlip.setTotalAmount(totalAmount);

        // ========== SET REQUIRED FIELDS WITH DEFAULTS ==========
        // Odometer reading (optional)
        if (request.getOdometerReading() != null) {
            fuelSlip.setOdometerReading(new BigDecimal(request.getOdometerReading().toString()));
        } else {
            fuelSlip.setOdometerReading(null); // Default null
        }

        // Location (required for auditing)
        fuelSlip.setLocation(request.getLocation() != null ? request.getLocation() : "Unknown Location");

        // Station name (required)
        fuelSlip.setStationName(request.getStationName());

        // Pump number (optional)
        fuelSlip.setPumpNumber(request.getPumpNumber());

        // Receipt number (optional, generate if not provided)
        if (request.getReceiptNumber() != null && !request.getReceiptNumber().trim().isEmpty()) {
            fuelSlip.setReceiptNumber(request.getReceiptNumber().trim());
        } else {
            fuelSlip.setReceiptNumber(generateReceiptNumber());
        }

        // Notes (optional)
        fuelSlip.setNotes(request.getNotes());

        // Fuel type (required)
        if (request.getFuelType() != null && !request.getFuelType().trim().isEmpty()) {
            fuelSlip.setFuelType(request.getFuelType());
        } else {
            fuelSlip.setFuelType("Diesel (50ppm)"); // Default fuel type
        }

        // Payment method (required)
        if (request.getPaymentMethod() != null && !request.getPaymentMethod().trim().isEmpty()) {
            fuelSlip.setPaymentMethod(request.getPaymentMethod());
        } else {
            fuelSlip.setPaymentMethod("Fleet Card"); // Default payment method
        }

        // ========== SET TRIP AND LOAD REFERENCES ==========
        fuelSlip.setTripId(request.getTripId());
        fuelSlip.setLoadId(request.getLoadId());

        // ========== SET STATUS FIELDS ==========
        // Finalized status (default to false)
        fuelSlip.setFinalized(request.getFinalized() != null ? request.getFinalized() : false);

        // Incident flag (default to false)
        fuelSlip.setIncidentFlag(false);

        // Last status update (set to now)
        fuelSlip.setLastStatusUpdate(LocalDateTime.now());

        // ========== SET VERIFICATION FIELDS ==========
        // Verified by (null by default, set when verified)
        fuelSlip.setVerifiedBy(null);

        // Verification date (null by default)
        fuelSlip.setVerificationDate(null);

        // ========== SET AUDIT TRAIL ==========
        Map<String, Object> auditTrail = new HashMap<>();
        auditTrail.put("createdAt", LocalDateTime.now().toString());
        auditTrail.put("createdBy", "fuel_slip_service");
        auditTrail.put("action", "CREATED");
        auditTrail.put("slipNumber", fuelSlip.getSlipNumber());
        auditTrail.put("initialStatus", fuelSlip.isFinalized() ? "FINALIZED" : "DRAFT");
        fuelSlip.setAuditTrail(auditTrail);

        // ========== SET ACCOUNT STATEMENT ==========
        // Null by default, will be linked later when reconciled
        fuelSlip.setAccountStatement(null);

        // ========== SET BASE ENTITY FIELDS ==========
        // These might be set automatically by @PrePersist, but set explicitly if needed
        fuelSlip.setCreatedAt(LocalDateTime.now());
        fuelSlip.setUpdatedAt(LocalDateTime.now());
        // createdBy and updatedBy might be set from security context

        // Save the fuel slip
        FuelSlip savedFuelSlip = fuelSlipRepository.save(fuelSlip);
        log.info("Fuel slip created successfully: {}", savedFuelSlip.getSlipNumber());

        return FuelSlipDTO.fromEntity(savedFuelSlip);
    }

    private void handleVehicleAssignment(FuelSlipRequest request, FuelSlip fuelSlip) {
        if (request.getVehicleId() != null) {
            // Scenario 2: Trip mode - use vehicle ID
            Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                    .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + request.getVehicleId()));
            fuelSlip.setVehicle(vehicle);
        } else if (request.getVehicleRegistration() != null && !request.getVehicleRegistration().trim().isEmpty()) {
            // Scenario 1: Manual mode - find by registration or create placeholder
            Optional<Vehicle> existingVehicle = vehicleRepository.findByRegistrationNumberIgnoreCase(
                    request.getVehicleRegistration().trim());

            if (existingVehicle.isPresent()) {
                fuelSlip.setVehicle(existingVehicle.get());
            } else {
                // Create a minimal vehicle record for manual entry
                Vehicle manualVehicle = new Vehicle();
                manualVehicle.setRegistrationNumber(request.getVehicleRegistration().trim().toUpperCase());
                manualVehicle.setMake("MANUAL_ENTRY");
                manualVehicle.setModel("UNKNOWN");

                Map<String, Object> auditMap = new HashMap<>();
                auditMap.put("createdBy", "fuel_slip_service");
                auditMap.put("note", "Manual entry for fuel slip");
                auditMap.put("createdAt", LocalDateTime.now().toString());
                manualVehicle.setAuditTrail(auditMap);

                manualVehicle.setStatus("ACTIVE");
                manualVehicle.setCreatedAt(LocalDateTime.now());
                manualVehicle.setUpdatedAt(LocalDateTime.now());

                Vehicle savedVehicle = vehicleRepository.save(manualVehicle);
                fuelSlip.setVehicle(savedVehicle);
                log.info("Created manual vehicle entry: {}", savedVehicle.getRegistrationNumber());
            }
        } else {
            throw new RuntimeException("Either vehicleId or vehicleRegistration must be provided");
        }
    }

    private void handleDriverAssignment(FuelSlipRequest request, FuelSlip fuelSlip) {
        if (request.getDriverId() != null) {
            // Scenario 2: Trip mode - use driver ID
            Driver driver = driverRepository.findById(request.getDriverId())
                    .orElseThrow(() -> new RuntimeException("Driver not found with id: " + request.getDriverId()));
            fuelSlip.setDriver(driver);
        } else if (request.getDriverName() != null && !request.getDriverName().trim().isEmpty()) {
            // Scenario 1: Manual mode - find by name or create placeholder
            String[] nameParts = request.getDriverName().trim().split(" ", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : "";

            // Search for driver by first and last name
            Optional<Driver> existingDriver = driverRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(
                    firstName, lastName);

            if (existingDriver.isPresent()) {
                fuelSlip.setDriver(existingDriver.get());
            } else {
                // Create a minimal driver record for manual entry
                Driver manualDriver = new Driver();
                manualDriver.setFirstName(firstName);
                manualDriver.setLastName(lastName);
                manualDriver.setLicenseNumber("MANUAL_" + System.currentTimeMillis());
                manualDriver.setStatus(DriverStatus.ACTIVE);
                manualDriver.setPhoneNumber("N/A");
                manualDriver.setEmail("manual_" + System.currentTimeMillis() + "@example.com");
                manualDriver.setCreatedAt(LocalDateTime.now());
                manualDriver.setUpdatedAt(LocalDateTime.now());

                Driver savedDriver = driverRepository.save(manualDriver);
                fuelSlip.setDriver(savedDriver);
                log.info("Created manual driver entry: {} {}", firstName, lastName);
            }
        } else {
            throw new RuntimeException("Either driverId or driverName must be provided");
        }
    }

    private void handleFuelSource(FuelSlipRequest request, FuelSlip fuelSlip) {
        // Try to find a default fuel source if not provided
        if (request.getFuelSourceId() != null) {
            FuelSource fuelSource = fuelSourceRepository.findById(request.getFuelSourceId())
                    .orElse(null);
            if (fuelSource != null) {
                fuelSlip.setFuelSource(fuelSource);
            }
        }

        // If no fuel source found, try to find a default one
        if (fuelSlip.getFuelSource() == null) {
            // You might have a method to find default fuel source
            List<FuelSource> defaultSources = fuelSourceRepository.findByNameContainingIgnoreCase("default");
            if (!defaultSources.isEmpty()) {
                fuelSlip.setFuelSource(defaultSources.get(0));
            } else {
                // Create a default fuel source if none exists
                FuelSource defaultSource = new FuelSource();
                defaultSource.setName("Default Fuel Source");
                defaultSource.setSourceType("FLEET_CARD"); // Set a default source type
                defaultSource.setAccountId(1L); // Set a default account ID or make it nullable

                defaultSource.setCreatedAt(LocalDateTime.now());
                defaultSource.setUpdatedAt(LocalDateTime.now());

                FuelSource savedSource = fuelSourceRepository.save(defaultSource);
                fuelSlip.setFuelSource(savedSource);
                log.info("Created default fuel source: {}", savedSource.getName());
            }
        }
    }

    private String generateSlipNumber() {
        LocalDateTime now = LocalDateTime.now();
        String prefix = "FS" + now.getYear() + String.format("%02d", now.getMonthValue());

        // Find the next sequence number for this month
        LocalDateTime startOfMonth = LocalDateTime.of(now.getYear(), now.getMonthValue(), 1, 0, 0, 0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusSeconds(1);

        Long count = fuelSlipRepository.countByTransactionDateBetween(startOfMonth, endOfMonth);
        long sequence = (count != null ? count : 0L) + 1;

        return String.format("%s-%04d", prefix, sequence);
    }

    private String generateReceiptNumber() {
        return "RCPT-" + System.currentTimeMillis();
    }

    private void validateFuelSlipRequest(FuelSlipRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new RuntimeException("Quantity must be provided and greater than 0");
        }
        if (request.getUnitPrice() == null || request.getUnitPrice() <= 0) {
            throw new RuntimeException("Unit price must be provided and greater than 0");
        }
        if (request.getStationName() == null || request.getStationName().trim().isEmpty()) {
            throw new RuntimeException("Station name is required");
        }
    }

    // Rest of the methods...
    public List<FuelSlipDTO> getFuelSlipsForPeriod(LocalDate start, LocalDate end) {
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        return fuelSlipRepository.findByTransactionDateBetween(startDateTime, endDateTime).stream()
                .map(FuelSlipDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public FuelSlipDTO getFuelSlipById(Long id) {
        FuelSlip fuelSlip = fuelSlipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fuel slip not found with id: " + id));
        return FuelSlipDTO.fromEntity(fuelSlip);
    }

    public List<FuelSlipDTO> getAllFuelSlips() {
        return fuelSlipRepository.findAll().stream()
                .map(FuelSlipDTO::fromEntity)
                .collect(Collectors.toList());
    }

   public List<FuelSlipDTO> getFuelSlipsByTripId(Long tripId) {
    log.info("🔍 FuelSlipService: Getting fuel slips for trip ID: {}", tripId);
    List<FuelSlip> slips = fuelSlipRepository.findByTripId(tripId);
    log.info("📊 Found {} fuel slips for trip {}", slips.size(), tripId);
    if (slips.isEmpty()) {
        log.warn("No fuel slips found for trip ID: {}", tripId);
    } else {
        for (FuelSlip slip : slips) {
            log.debug("Slip ID: {}, Trip ID from entity: {}", 
                slip.getId(), slip.getTripId());
            if (!tripId.equals(slip.getTripId())) {
                log.warn("Repository returned slip {} with wrong tripId: {}", 
                    slip.getId(), slip.getTripId());
            }
        }
    }
    
    return slips.stream()
            .map(FuelSlipDTO::fromEntity)
            .collect(Collectors.toList());
}

    public List<FuelSlipDTO> getFuelSlipsByDriver(Long driverId) {
        return fuelSlipRepository.findByDriverId(driverId).stream()
                .map(FuelSlipDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<FuelSlipDTO> getFuelSlipsByVehicle(Long vehicleId) {
        return fuelSlipRepository.findByVehicleId(vehicleId).stream()
                .map(FuelSlipDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<FuelSlipDTO> getFuelSlipsByDriverAndVehicle(Long driverId, Long vehicleId) {
        return fuelSlipRepository.findByDriverIdAndVehicleId(driverId, vehicleId).stream()
                .map(FuelSlipDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public FuelSlipDTO updateFuelSlip(Long id, FuelSlipRequest request) {
        FuelSlip existing = fuelSlipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fuel slip not found with id: " + id));

        if (existing.isFinalized()) {
            throw new RuntimeException("Cannot update a finalized fuel slip");
        }

        // Update fields
        if (request.getQuantity() != null) {
            existing.setQuantity(new BigDecimal(request.getQuantity().toString()));
        }
        if (request.getUnitPrice() != null) {
            existing.setUnitPrice(new BigDecimal(request.getUnitPrice().toString()));
        }
        if (request.getQuantity() != null && request.getUnitPrice() != null) {
            BigDecimal total = new BigDecimal(request.getQuantity().toString())
                    .multiply(new BigDecimal(request.getUnitPrice().toString()));
            existing.setTotalAmount(total);
        }

        if (request.getOdometerReading() != null) {
            existing.setOdometerReading(new BigDecimal(request.getOdometerReading().toString()));
        }

        if (request.getLocation() != null) {
            existing.setLocation(request.getLocation());
        }
        if (request.getStationName() != null) {
            existing.setStationName(request.getStationName());
        }
        if (request.getNotes() != null) {
            existing.setNotes(request.getNotes());
        }

        if (request.getFuelType() != null) {
            existing.setFuelType(request.getFuelType());
        }

        if (request.getPaymentMethod() != null) {
            existing.setPaymentMethod(request.getPaymentMethod());
        }

        // Update audit trail
        Map<String, Object> auditTrail = existing.getAuditTrail() != null ? existing.getAuditTrail() : new HashMap<>();
        auditTrail.put("lastUpdated", LocalDateTime.now().toString());
        auditTrail.put("updateAction", "UPDATED");
        existing.setAuditTrail(auditTrail);

        existing.setUpdatedAt(LocalDateTime.now());
        existing.setLastStatusUpdate(LocalDateTime.now());

        FuelSlip updated = fuelSlipRepository.save(existing);
        return FuelSlipDTO.fromEntity(updated);
    }

    public void deleteFuelSlip(Long id) {
        FuelSlip existing = fuelSlipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fuel slip not found with id: " + id));

        if (existing.isFinalized()) {
            throw new RuntimeException("Cannot delete a finalized fuel slip");
        }

        fuelSlipRepository.deleteById(id);
    }

    @Transactional
    public void finalizeFuelSlip(Long id) {
        FuelSlip existing = fuelSlipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fuel slip not found with id: " + id));

        if (existing.isFinalized()) {
            throw new RuntimeException("Fuel slip is already finalized");
        }

        existing.setFinalized(true);
        existing.setLastStatusUpdate(LocalDateTime.now());

        // Update audit trail
        Map<String, Object> auditTrail = existing.getAuditTrail() != null ? existing.getAuditTrail() : new HashMap<>();
        auditTrail.put("finalizedAt", LocalDateTime.now().toString());
        auditTrail.put("finalizedBy", "system");
        existing.setAuditTrail(auditTrail);

        existing.setUpdatedAt(LocalDateTime.now());
        fuelSlipRepository.save(existing);
    }

    @Transactional
    public void verifyFuelSlip(Long id, String verifiedBy) {
        FuelSlip existing = fuelSlipRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fuel slip not found with id: " + id));

        existing.setVerifiedBy(verifiedBy);
        existing.setVerificationDate(LocalDateTime.now());
        existing.setLastStatusUpdate(LocalDateTime.now());

        // Update audit trail
        Map<String, Object> auditTrail = existing.getAuditTrail() != null ? existing.getAuditTrail() : new HashMap<>();
        auditTrail.put("verifiedAt", LocalDateTime.now().toString());
        auditTrail.put("verifiedBy", verifiedBy);
        existing.setAuditTrail(auditTrail);

        existing.setUpdatedAt(LocalDateTime.now());
        fuelSlipRepository.save(existing);
    }
}
