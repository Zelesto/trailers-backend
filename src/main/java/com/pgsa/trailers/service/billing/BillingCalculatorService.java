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

    /**
     * Calculate billing for a single trip
     */
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

            // 2. Get trip metrics - handle nulls properly
            // Distance: Use calculated or actual distance
            BigDecimal distance = trip.getActualDistanceKm() != null && trip.getActualDistanceKm().compareTo(BigDecimal.ZERO) > 0
                    ? trip.getActualDistanceKm()
                    : trip.getCalculatedDistanceKm();
            if (distance == null || distance.compareTo(BigDecimal.ZERO) <= 0) {
                distance = trip.getPlannedDistanceKm() != null ? trip.getPlannedDistanceKm() : BigDecimal.ZERO;
            }

            // Tonnage: Convert kg to tons (divide by 1000)
            BigDecimal weightKg = trip.getCargoWeight() != null ? trip.getCargoWeight() : BigDecimal.ZERO;
            BigDecimal tonnage = weightKg.divide(TON_CONVERSION, 2, RoundingMode.HALF_UP);

            // Days: Calculate from planned dates or default to 1
            Integer days = calculateTripDays(trip);

            // Labour hours: Estimate based on commodity and distance
            BigDecimal labourHours = estimateLabourHours(trip);

            // Crane hours: Default to 0 unless crane was used
            BigDecimal craneHours = Boolean.TRUE.equals(trip.getCraneUsed()) ? new BigDecimal("2.0") : BigDecimal.ZERO;

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

    /**
     * Calculate distance charge with sliding scale
     */
    private BigDecimal calculateDistanceCharge(Rate rate, BigDecimal distance) {
        if (rate.getPerKm() == null || distance == null || distance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal ratePerKm = rate.getPerKm();

        // Apply sliding scale discounts for longer distances
        if (distance.compareTo(new BigDecimal("500")) > 0) {
            ratePerKm = ratePerKm.multiply(new BigDecimal("0.90")); // 10% discount
        } else if (distance.compareTo(new BigDecimal("200")) > 0) {
            ratePerKm = ratePerKm.multiply(new BigDecimal("0.95")); // 5% discount
        }

        return distance.multiply(ratePerKm).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate tonnage charge (tonnage in tons)
     */
    private BigDecimal calculateTonnageCharge(Rate rate, BigDecimal tonnage) {
        if (rate.getPerTon() == null || tonnage == null || tonnage.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return tonnage.multiply(rate.getPerTon()).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate daily rate charge
     */
    private BigDecimal calculateDailyRateCharge(Rate rate, Integer days) {
        if (rate.getPerDay() == null || days == null || days <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(days).multiply(rate.getPerDay()).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate labour charge
     */
    private BigDecimal calculateLabourCharge(Rate rate, BigDecimal labourHours) {
        if (rate.getLabourHourlyRate() == null || labourHours == null || labourHours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return labourHours.multiply(rate.getLabourHourlyRate()).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate crane charge
     */
    private BigDecimal calculateCraneCharge(Trip trip) {
        // First check the crane_used field
        Boolean craneUsed = trip.getCraneUsed();
        if (craneUsed != null && craneUsed) {
            log.info("Crane used for trip: {}", trip.getTripNumber());
            
            // If we have crane hours field
            try {
                Double craneHours = trip.getCraneHours();
                if (craneHours != null && craneHours > 0) {
                    return CRANE_BASE_CHARGE.add(CRANE_HOURLY_RATE.multiply(BigDecimal.valueOf(craneHours)))
                            .setScale(2, RoundingMode.HALF_UP);
                }
            } catch (Exception e) {
                // craneHours field doesn't exist
                log.debug("No crane hours field, using base charge only");
            }
            return CRANE_BASE_CHARGE;
        }
        
        // If crane_used is false or null, check trip_type as fallback
        String tripType = trip.getTripType();
        if (tripType != null && tripType.toUpperCase().contains("CRANE")) {
            log.info("Crane detected in trip_type: {}", tripType);
            return CRANE_BASE_CHARGE;
        }
        
        return BigDecimal.ZERO;
    }

    /**
     * Estimate labour hours based on commodity and distance
     */
    private BigDecimal estimateLabourHours(Trip trip) {
        BigDecimal defaultHours = new BigDecimal("2.0");

        // Check commodity type for multiplier
        if (trip.getCommodityType() != null) {
            String commodity = trip.getCommodityType().toLowerCase();
            if (commodity.contains("transformer") || commodity.contains("heavy")) {
                return new BigDecimal("4.0");
            }
            if (commodity.contains("chemical") || commodity.contains("hazardous")) {
                return new BigDecimal("3.5");
            }
            if (commodity.contains("drum") || commodity.contains("barrel")) {
                return new BigDecimal("2.5");
            }
            if (commodity.contains("pallet")) {
                return new BigDecimal("2.0");
            }
        }

        // Distance factor (longer trips need more labour)
        BigDecimal distance = trip.getCalculatedDistanceKm() != null 
                ? trip.getCalculatedDistanceKm() 
                : trip.getPlannedDistanceKm();
        
        if (distance != null) {
            if (distance.compareTo(new BigDecimal("1000")) > 0) {
                return new BigDecimal("4.0");
            }
            if (distance.compareTo(new BigDecimal("500")) > 0) {
                return new BigDecimal("3.0");
            }
            if (distance.compareTo(new BigDecimal("200")) > 0) {
                return new BigDecimal("2.5");
            }
        }

        return defaultHours;
    }

    /**
     * Calculate number of days for daily rate
     */
    private Integer calculateTripDays(Trip trip) {
        // Use actual dates if available
        if (trip.getActualStartDate() != null && trip.getActualEndDate() != null) {
            long days = Duration.between(trip.getActualStartDate(), trip.getActualEndDate()).toDays();
            return Math.max(1, (int) days);
        }

        // Use planned dates
        if (trip.getPlannedStartDate() != null && trip.getPlannedEndDate() != null) {
            long days = Duration.between(trip.getPlannedStartDate(), trip.getPlannedEndDate()).toDays();
            return Math.max(1, (int) days);
        }

        // Use estimated duration hours
        if (trip.getEstimatedDurationHours() != null) {
            int days = trip.getEstimatedDurationHours().divide(new BigDecimal("24"), 0, RoundingMode.HALF_UP).intValue();
            return Math.max(1, days);
        }

        return 1;
    }

    /**
     * Create a default rate with sensible defaults
     */
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
        try {
            Trip trip = tripRepository.findById(tripId)
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
                    .build();
            billing.calculateTotals();
            return tripBillingRepository.save(billing);
        } catch (Exception e) {
            log.error("❌ Even default billing failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get billing for a load
     */
    public LoadBilling getLoadBilling(String loadId) {
        return loadBillingRepository.findByLoadId(loadId)
                .orElseThrow(() -> new RuntimeException("Load billing not found: " + loadId));
    }

    /**
     * Get billing for a trip
     */
    public TripBilling getTripBilling(Long tripId) {
        TripBilling billing = tripBillingRepository.findByTripId(tripId);
        if (billing == null) {
            throw new RuntimeException("Trip billing not found for trip: " + tripId);
        }
        return billing;
    }

    /**
     * Get all billable loads
     */
    public List<LoadBilling> getBillableLoads() {
        try {
            return loadBillingRepository.findBillableLoads();
        } catch (Exception e) {
            log.error("Error fetching billable loads: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Update load billing
     */
    @Transactional
    public LoadBilling updateLoadBilling(String loadId, Long userId) {
        log.info("📦 Updating billing for Load: {}", loadId);

        Load load = loadRepository.findByLoadNumber(loadId)
                .orElseThrow(() -> new RuntimeException("Load not found: " + loadId));

        List<Trip> trips = tripRepository.findByLoadId(loadId);
        List<TripBilling> billings = tripBillingRepository.findByTrip_LoadId(loadId);

        LoadBilling loadBilling = loadBillingRepository.findByLoadId(loadId)
                .orElse(LoadBilling.builder().loadId(loadId).build());

        // Aggregate metrics
        loadBilling.setCustomerId(load.getCustomerId());
        loadBilling.setTotalTrips(trips.size());

        // Total distance
        BigDecimal totalDistance = trips.stream()
                .map(t -> t.getCalculatedDistanceKm() != null ? t.getCalculatedDistanceKm() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        loadBilling.setTotalDistanceKm(totalDistance);

        // Total tonnage (convert kg to tons)
        BigDecimal totalTonnage = trips.stream()
                .map(t -> t.getCargoWeight() != null ? t.getCargoWeight().divide(TON_CONVERSION, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        loadBilling.setTotalTonnage(totalTonnage);

        // Aggregate charges from billings
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

        // Calculate totals
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

    /**
     * Recalculate all billings for a load
     */
    @Transactional
    public LoadBilling recalculateLoadBilling(String loadId, Long userId) {
        log.info("🔄 Recalculating all billings for Load: {}", loadId);

        List<Trip> trips = tripRepository.findByLoadId(loadId);

        for (Trip trip : trips) {
            tripBillingRepository.deleteByTripId(trip.getId());
            calculateTripBilling(trip.getId(), userId);
        }

        return updateLoadBilling(loadId, userId);
    }

    /**
     * Calculate billing in a new transaction (for trip completion)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TripBilling calculateTripBillingInNewTransaction(Long tripId, Long userId) {
        log.info("💰 Calculating billing in new transaction for Trip: {}", tripId);
        return calculateTripBilling(tripId, userId);
    }
}
