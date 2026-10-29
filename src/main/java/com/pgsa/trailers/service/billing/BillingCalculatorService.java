// src/main/java/com/pgsa/trailers/service/billing/BillingCalculatorService.java

package com.pgsa.trailers.service.billing;

import com.pgsa.trailers.entity.billing.LoadBilling;
import com.pgsa.trailers.entity.billing.Rate;
import com.pgsa.trailers.entity.billing.TripBilling;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.billing.TripBillingRepository;
import com.pgsa.trailers.repository.billing.LoadBillingRepository;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingCalculatorService {

    private final RateResolverService rateResolverService;
    private final TripBillingRepository tripBillingRepository;
    private final LoadBillingRepository loadBillingRepository;
    private final LoadRepository loadRepository;
    private final TripRepository tripRepository;

    private static final BigDecimal VAT_RATE = new BigDecimal("0.15");
    private static final BigDecimal TON_CONVERSION = new BigDecimal("1000");
    private static final BigDecimal CRANE_BASE_CHARGE = new BigDecimal("150.00");

    // ==================== MAIN CALCULATION METHOD ====================

    @Transactional
    public TripBilling calculateTripBilling(Long tripId, Long userId) {
        log.info("💰 Calculating billing for Trip ID: {}", tripId);

        try {
            Trip trip = tripRepository.findByIdWithAllRelations(tripId)
                    .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));

            // Check if already calculated
            TripBilling existing = tripBillingRepository.findByTripId(tripId);
            if (existing != null && "CALCULATED".equals(existing.getStatus())) {
                log.info("Trip {} already has billing calculated", tripId);
                return existing;
            }

            // 1. Resolve rate
            Rate rate = rateResolverService.resolveRate(trip);
            if (rate == null) {
                log.warn("⚠️ No rate found for trip {}, using default", tripId);
                rate = createDefaultRate();
            }

            // 2. Get trip metrics
            BigDecimal distance = getTripDistance(trip);
            BigDecimal tonnage = getTripTonnage(trip);
            Integer days = calculateTripDays(trip);
            BigDecimal labourHours = estimateLabourHours(trip);
            BigDecimal craneHours = getCraneHours(trip);

            // 3. Calculate each component
            BigDecimal distanceCharge = calculateDistanceCharge(rate, distance);
            BigDecimal tonnageCharge = calculateTonnageCharge(rate, tonnage);
            BigDecimal dailyRateCharge = calculateDailyRateCharge(rate, days);
            BigDecimal labourCharge = calculateLabourCharge(rate, labourHours);
            BigDecimal craneCharge = calculateCraneCharge(rate, craneHours);
            BigDecimal fixedSurcharge = rate.getFixedAmount() != null ? rate.getFixedAmount() : BigDecimal.ZERO;

            // 4. Build TripBilling
            TripBilling billing = TripBilling.builder()
                    .trip(trip)
                    .rate(rate)
                    .customerId(trip.getCustomerId() != null ? trip.getCustomerId() : 0L)
                    .distanceKm(distance)
                    .tonnage(tonnage)
                    .days(days)
                    .labourHours(labourHours)
                    .craneHours(craneHours)
                    .baseRate(rate.getPerKm() != null ? rate.getPerKm() : BigDecimal.ZERO)
                    .distanceCharge(distanceCharge)
                    .tonnageCharge(tonnageCharge)
                    .dailyRateCharge(dailyRateCharge)
                    .labourCharge(labourCharge)
                    .craneCharge(craneCharge)
                    .fixedSurcharge(fixedSurcharge)
                    .status("CALCULATED")
                    .calculatedAt(LocalDateTime.now())
                    .calculatedBy(userId)
                    .isVatApplicable(true)
                    .vatRate(new BigDecimal("15.00"))
                    .build();

            billing.calculateTotals();
            TripBilling saved = tripBillingRepository.save(billing);

            log.info("✅ Trip billing calculated: {} | Distance: {} km | Tonnage: {} tons | Total: {}",
                    trip.getTripNumber(), distance, tonnage, saved.getTotal());

            // 5. Update load billing
            if (trip.getLoadId() != null && !trip.getLoadId().isEmpty()) {
                updateLoadBilling(trip.getLoadId(), userId);
            }

            return saved;

        } catch (Exception e) {
            log.error("❌ Error calculating billing for trip {}: {}", tripId, e.getMessage(), e);
            return createDefaultTripBilling(tripId, userId);
        }
    }

    // ==================== HELPER METHODS ====================

    private BigDecimal getTripDistance(Trip trip) {
        BigDecimal distance = trip.getActualDistanceKm() != null && trip.getActualDistanceKm().compareTo(BigDecimal.ZERO) > 0
                ? trip.getActualDistanceKm()
                : trip.getCalculatedDistanceKm();
        if (distance == null || distance.compareTo(BigDecimal.ZERO) <= 0) {
            distance = trip.getPlannedDistanceKm() != null ? trip.getPlannedDistanceKm() : BigDecimal.ZERO;
        }
        return distance;
    }

    private BigDecimal getTripTonnage(Trip trip) {
        BigDecimal weightKg = trip.getCargoWeight() != null ? trip.getCargoWeight() : BigDecimal.ZERO;
        return weightKg.divide(TON_CONVERSION, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal getCraneHours(Trip trip) {
        if (Boolean.TRUE.equals(trip.getCraneUsed())) {
            return BigDecimal.ONE; // Default 1 hour if crane was used
        }
        return BigDecimal.ZERO;
    }

    private Integer calculateTripDays(Trip trip) {
        if (trip.getActualStartDate() != null && trip.getActualEndDate() != null) {
            long days = Duration.between(trip.getActualStartDate(), trip.getActualEndDate()).toDays();
            return Math.max(1, (int) days);
        }
        if (trip.getPlannedStartDate() != null && trip.getPlannedEndDate() != null) {
            long days = Duration.between(trip.getPlannedStartDate(), trip.getPlannedEndDate()).toDays();
            return Math.max(1, (int) days);
        }
        if (trip.getEstimatedDurationHours() != null) {
            int days = trip.getEstimatedDurationHours().divide(new BigDecimal("24"), 0, RoundingMode.HALF_UP).intValue();
            return Math.max(1, days);
        }
        return 1;
    }

    private BigDecimal estimateLabourHours(Trip trip) {
        BigDecimal defaultHours = new BigDecimal("2.0");

        if (trip.getCommodityType() != null) {
            String commodity = trip.getCommodityType().toLowerCase();
            if (commodity.contains("transformer") || commodity.contains("heavy")) {
                return new BigDecimal("4.0");
            }
            if (commodity.contains("chemical") || commodity.contains("hazardous")) {
                return new BigDecimal("3.5");
            }
        }

        BigDecimal distance = trip.getCalculatedDistanceKm() != null 
                ? trip.getCalculatedDistanceKm() 
                : trip.getPlannedDistanceKm();
        
        if (distance != null) {
            if (distance.compareTo(new BigDecimal("1000")) > 0) return new BigDecimal("4.0");
            if (distance.compareTo(new BigDecimal("500")) > 0) return new BigDecimal("3.0");
            if (distance.compareTo(new BigDecimal("200")) > 0) return new BigDecimal("2.5");
        }

        return defaultHours;
    }
/**
 * Resolves a Load by:
 *   1. loadNumber (string business key, e.g. "LOAD-2026-001")
 *   2. numeric DB id (fallback, e.g. "3")
 *
 * This makes endpoints tolerant of either value being sent from the frontend.
 */
private Load resolveLoad(String loadIdOrNumber) {
    if (loadIdOrNumber == null || loadIdOrNumber.isBlank()) {
        throw new RuntimeException("loadId is required");
    }

    // 1. Try loadNumber first (business key — always a String)
    var byNumber = loadRepository.findByLoadNumber(loadIdOrNumber);
    if (byNumber.isPresent()) {
        log.info("  → resolved load by loadNumber: {}", loadIdOrNumber);
        return byNumber.get();
    }

    // 2. Fallback: numeric DB primary key
    try {
        Long numericId = Long.parseLong(loadIdOrNumber);
        Load load = loadRepository.findById(numericId)
                .orElseThrow(() -> new RuntimeException(
                    "Load not found by loadNumber '" + loadIdOrNumber +
                    "' or id " + numericId));
        log.info("  → resolved load by numeric id: {} → loadNumber: {}",
                numericId, load.getLoadNumber());
        return load;
    } catch (NumberFormatException e) {
        throw new RuntimeException("Load not found: " + loadIdOrNumber);
    }
}


    /**
 * Recalculate all billings for a load
 */
@Transactional
public LoadBilling recalculateLoadBilling(String loadId, Long userId) {
    log.info("🔄 Recalculating billing for Load: {}", loadId);

    // Resolve to a canonical load + canonical loadNumber
    Load load = resolveLoad(loadId);
    String canonicalLoadNumber = load.getLoadNumber();
    log.info("  → canonical loadNumber: {}", canonicalLoadNumber);

    // Delete existing trip billings for this load
    List<Trip> trips = tripRepository.findByLoadId(canonicalLoadNumber);

    for (Trip trip : trips) {
        TripBilling existing = tripBillingRepository.findByTripId(trip.getId());
        if (existing != null) {
            tripBillingRepository.delete(existing);
        }
        calculateTripBilling(trip.getId(), userId);
    }

    // Update the LoadBilling — use the canonical loadNumber so the
    // LoadBilling.loadId field matches what findByLoadId queries use
    return updateLoadBilling(canonicalLoadNumber, userId);
}
    
    // ==================== CHARGE CALCULATIONS ====================

    private BigDecimal calculateDistanceCharge(Rate rate, BigDecimal distance) {
        if (rate.getPerKm() == null || distance == null || distance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal ratePerKm = rate.getPerKm();
        if (distance.compareTo(new BigDecimal("500")) > 0) {
            ratePerKm = ratePerKm.multiply(new BigDecimal("0.90"));
        } else if (distance.compareTo(new BigDecimal("200")) > 0) {
            ratePerKm = ratePerKm.multiply(new BigDecimal("0.95"));
        }
        return distance.multiply(ratePerKm).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTonnageCharge(Rate rate, BigDecimal tonnage) {
        if (rate.getPerTon() == null || tonnage == null || tonnage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return tonnage.multiply(rate.getPerTon()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDailyRateCharge(Rate rate, Integer days) {
        if (rate.getPerDay() == null || days == null || days <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(days).multiply(rate.getPerDay()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateLabourCharge(Rate rate, BigDecimal labourHours) {
        if (rate.getLabourHourlyRate() == null || labourHours == null || labourHours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return labourHours.multiply(rate.getLabourHourlyRate()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCraneCharge(Rate rate, BigDecimal craneHours) {
        if (craneHours == null || craneHours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal craneRate = rate.getCraneHourlyRate() != null 
                ? rate.getCraneHourlyRate() 
                : CRANE_BASE_CHARGE;
        return craneHours.multiply(craneRate).setScale(2, RoundingMode.HALF_UP);
    }

    // ==================== DEFAULT CREATION ====================

    private Rate createDefaultRate() {
        Rate defaultRate = new Rate();
        defaultRate.setPerKm(new BigDecimal("1.50"));
        defaultRate.setPerTon(new BigDecimal("50.00"));
        defaultRate.setPerDay(new BigDecimal("2500.00"));
        defaultRate.setLabourHourlyRate(new BigDecimal("200.00"));
        defaultRate.setCraneHourlyRate(new BigDecimal("350.00"));
        defaultRate.setFixedAmount(BigDecimal.ZERO);
        defaultRate.setPriority(0);
        return defaultRate;
    }

    /**
     * Create a default TripBilling when calculation fails
     */
    private TripBilling createDefaultTripBilling(Long tripId, Long userId) {
        log.info("📝 Creating default billing for Trip ID: {}", tripId);
        try {
            Trip trip = tripRepository.findByIdWithAllRelations(tripId)
                    .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));

            TripBilling billing = TripBilling.builder()
                    .trip(trip)
                    .customerId(trip.getCustomerId() != null ? trip.getCustomerId() : 0L)
                    .distanceKm(BigDecimal.ZERO)
                    .tonnage(BigDecimal.ZERO)
                    .days(1)
                    .labourHours(BigDecimal.ZERO)
                    .craneHours(BigDecimal.ZERO)
                    .baseRate(BigDecimal.ZERO)
                    .distanceCharge(BigDecimal.ZERO)
                    .tonnageCharge(BigDecimal.ZERO)
                    .dailyRateCharge(BigDecimal.ZERO)
                    .labourCharge(BigDecimal.ZERO)
                    .craneCharge(BigDecimal.ZERO)
                    .fixedSurcharge(BigDecimal.ZERO)
                    .status("DRAFT")
                    .calculatedAt(LocalDateTime.now())
                    .calculatedBy(userId)
                    .isVatApplicable(true)
                    .vatRate(new BigDecimal("15.00"))
                    .build();
            
            // ✅ Calculate totals using the existing method
            billing.calculateTotals();
            
            TripBilling saved = tripBillingRepository.save(billing);
            log.info("✅ Default billing created for trip {}: Total = {}", tripId, saved.getTotal());
            return saved;
            
        } catch (Exception e) {
            log.error("❌ Default billing creation failed for trip {}: {}", tripId, e.getMessage(), e);
            return null;
        }
    }

    // ==================== LOAD BILLING METHODS ====================

public LoadBilling updateLoadBilling(String loadId, Long userId) {
    log.info("📦 Updating billing for Load: {}", loadId);

    // Use the shared resolver — tolerant of both loadNumber and numeric id
    Load load = resolveLoad(loadId);
    String canonicalLoadNumber = load.getLoadNumber();
    log.info("  → using loadNumber: {}", canonicalLoadNumber);

    List<Trip> trips = tripRepository.findByLoadId(canonicalLoadNumber);
    List<TripBilling> billings = tripBillingRepository.findByTrip_LoadId(canonicalLoadNumber);

    LoadBilling loadBilling = loadBillingRepository.findByLoadId(canonicalLoadNumber)
            .orElse(LoadBilling.builder().loadId(canonicalLoadNumber).build());

    loadBilling.setCustomerId(load.getCustomerId());
    loadBilling.setTotalTrips(trips.size());

    BigDecimal totalDistance = trips.stream()
            .map(t -> t.getCalculatedDistanceKm() != null ? t.getCalculatedDistanceKm() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    loadBilling.setTotalDistanceKm(totalDistance);

    BigDecimal totalTonnage = trips.stream()
            .map(t -> t.getCargoWeight() != null
                    ? t.getCargoWeight().divide(TON_CONVERSION, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    loadBilling.setTotalTonnage(totalTonnage);

    loadBilling.setTotalDistanceCharge(billings.stream()
            .map(TripBilling::getDistanceCharge)
            .filter(c -> c != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    loadBilling.setTotalTonnageCharge(billings.stream()
            .map(TripBilling::getTonnageCharge)
            .filter(c -> c != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    loadBilling.setTotalDailyCharge(billings.stream()
            .map(TripBilling::getDailyRateCharge)
            .filter(c -> c != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    loadBilling.setTotalLabourCharge(billings.stream()
            .map(TripBilling::getLabourCharge)
            .filter(c -> c != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    loadBilling.setTotalCraneCharge(billings.stream()
            .map(TripBilling::getCraneCharge)
            .filter(c -> c != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    loadBilling.setTotalFixedSurcharge(billings.stream()
            .map(TripBilling::getFixedSurcharge)
            .filter(c -> c != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

    BigDecimal subtotal = BigDecimal.ZERO;
    subtotal = subtotal.add(loadBilling.getTotalDistanceCharge());
    subtotal = subtotal.add(loadBilling.getTotalTonnageCharge());
    subtotal = subtotal.add(loadBilling.getTotalDailyCharge());
    subtotal = subtotal.add(loadBilling.getTotalLabourCharge());
    subtotal = subtotal.add(loadBilling.getTotalCraneCharge());
    subtotal = subtotal.add(loadBilling.getTotalFixedSurcharge());

    loadBilling.setSubtotal(subtotal);
    loadBilling.setVat(subtotal.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP));
    loadBilling.setTotal(subtotal.add(loadBilling.getVat()));

    if (loadBilling.getStatus() == null) {
        loadBilling.setStatus("CALCULATED");
    }
    loadBilling.setCalculatedAt(LocalDateTime.now());
    loadBilling.setCalculatedBy(userId);

    return loadBillingRepository.save(loadBilling);
}

    // ==================== QUERY METHODS ====================

   public LoadBilling getLoadBilling(String loadId) {
    // Try the string loadId first (the "correct" business key)
    var byLoadId = loadBillingRepository.findByLoadId(loadId);
    if (byLoadId.isPresent()) return byLoadId.get();

    // Fallback: numeric loadBilling PK
    try {
        Long numericId = Long.parseLong(loadId);
        return loadBillingRepository.findById(numericId)
                .orElseThrow(() -> new RuntimeException("Load billing not found: " + loadId));
    } catch (NumberFormatException e) {
        throw new RuntimeException("Load billing not found: " + loadId);
    }
}

    public TripBilling getTripBilling(Long tripId) {
        TripBilling billing = tripBillingRepository.findByTripId(tripId);
        if (billing == null) {
            throw new RuntimeException("Trip billing not found for trip: " + tripId);
        }
        return billing;
    }

    public List<LoadBilling> getBillableLoads() {
        try {
            List<LoadBilling> billableLoads = loadBillingRepository.findBillableLoads();
            if (billableLoads != null && !billableLoads.isEmpty()) {
                return billableLoads;
            }

            List<LoadBilling> generatedLoads = new ArrayList<>();
            List<Load> loads = loadRepository.findAll();

            for (Load load : loads) {
                if (load == null || load.getLoadNumber() == null || load.getLoadNumber().isBlank()) {
                    continue;
                }

                try {
                    List<Trip> trips = tripRepository.findByLoadId(load.getLoadNumber());
                    if (trips == null || trips.isEmpty()) {
                        continue;
                    }

                    LoadBilling loadBilling = loadBillingRepository.findByLoadId(load.getLoadNumber())
                            .orElse(null);

                    if (loadBilling == null) {
                        loadBilling = recalculateLoadBilling(load.getLoadNumber(), 0L);
                    }

                    if (loadBilling != null
                            && loadBilling.getInvoiceId() == null
                            && ("CALCULATED".equalsIgnoreCase(loadBilling.getStatus())
                                || "DRAFT".equalsIgnoreCase(loadBilling.getStatus()))) {
                        generatedLoads.add(loadBilling);
                    }
                } catch (Exception e) {
                    log.warn("Failed to backfill billable load {}: {}", load.getLoadNumber(), e.getMessage());
                }
            }

            return generatedLoads;
        } catch (Exception e) {
            log.error("Error fetching billable loads: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TripBilling calculateTripBillingInNewTransaction(Long tripId, Long userId) {
        log.info("💰 Calculating billing in new transaction for Trip: {}", tripId);
        return calculateTripBilling(tripId, userId);
    }
}
