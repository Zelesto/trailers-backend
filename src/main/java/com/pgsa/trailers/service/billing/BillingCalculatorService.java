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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingCalculatorService {

    private final RateResolverService rateResolverService;
    private final TripBillingRepository tripBillingRepository;
    private final LoadBillingRepository loadBillingRepository;
    private final LoadRepository loadRepository;
    private final TripRepository tripRepository;

    /**
     * Calculate billing for a single trip
     */
    @Transactional
    public TripBilling calculateTripBilling(Long tripId, Long userId) {
        log.info("💰 Calculating billing for Trip ID: {}", tripId);

        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));

        // Check if already calculated
        TripBilling existing = tripBillingRepository.findByTripId(tripId);
        if (existing != null && "CALCULATED".equals(existing.getStatus())) {
            log.info("Trip {} already has billing calculated", tripId);
            return existing;
        }

        // 1. Resolve rate
        Rate rate = rateResolverService.resolveRate(trip);

        // 2. Get trip metrics
        BigDecimal distance = trip.getCalculatedDistanceKm() != null 
            ? trip.getCalculatedDistanceKm() 
            : BigDecimal.ZERO;

        BigDecimal tonnage = trip.getCargoWeight() != null 
            ? trip.getCargoWeight() 
            : BigDecimal.ZERO;

        // Labour hours from commodity or default
        BigDecimal labourHours = estimateLabourHours(trip);

        // Crane hours (default 0, can be extended)
        BigDecimal craneHours = BigDecimal.ZERO;

        // Days - calculate from planned dates
        Integer days = calculateTripDays(trip);

        // 3. Calculate each component
        BigDecimal distanceCharge = calculateDistanceCharge(rate, distance);
        BigDecimal tonnageCharge = calculateTonnageCharge(rate, tonnage);
        BigDecimal dailyRateCharge = calculateDailyRateCharge(rate, days);
        BigDecimal labourCharge = calculateLabourCharge(rate, labourHours);
        BigDecimal craneCharge = calculateCraneCharge(rate, craneHours);
        BigDecimal fixedSurcharge = rate.getFixedAmount() != null ? rate.getFixedAmount() : BigDecimal.ZERO;

        // 4. Get sliding tier applied (for audit)
        Map<String, Object> slidingTier = getSlidingTier(rate, distance);

        // 5. Build TripBilling
        TripBilling billing = TripBilling.builder()
            .trip(trip)
            .rate(rate)
            .customer(trip.getCustomer())
            .distanceKm(distance)
            .tonnage(tonnage)
            .days(days)
            .labourHours(labourHours)
            .craneHours(craneHours)
            .baseRate(rate.getPerKm())
            .distanceCharge(distanceCharge)
            .tonnageCharge(tonnageCharge)
            .dailyRateCharge(dailyRateCharge)
            .labourCharge(labourCharge)
            .craneCharge(craneCharge)
            .fixedSurcharge(fixedSurcharge)
            .appliedSlidingTier(slidingTier)
            .status("CALCULATED")
            .calculatedAt(LocalDateTime.now())
            .calculatedBy(userId)
            .build();

        // 6. Calculate totals
        billing.calculateTotals();

        // 7. Save
        TripBilling saved = tripBillingRepository.save(billing);
        log.info("✅ Trip billing calculated: {} total = {}", trip.getTripNumber(), saved.getTotal());

        // 8. Update load billing
        if (trip.getLoadId() != null && !trip.getLoadId().isEmpty()) {
            updateLoadBilling(trip.getLoadId(), userId);
        }

        return saved;
    }

    /**
     * Calculate distance charge with sliding scale
     */
    private BigDecimal calculateDistanceCharge(Rate rate, BigDecimal distance) {
        if (rate.getPerKm() == null || distance == null || distance.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal ratePerKm = rate.getPerKm();

        // Check sliding scale
        if (rate.getSlidingScale() != null) {
            Map<String, Object> scale = rate.getSlidingScale();
            List<Map<String, Object>> tiers = (List<Map<String, Object>>) scale.get("tiers");
            
            if (tiers != null) {
                for (Map<String, Object> tier : tiers) {
                    BigDecimal uptoKm = new BigDecimal(tier.get("upto_km").toString());
                    BigDecimal tierRate = new BigDecimal(tier.get("rate").toString());
                    
                    if (distance.compareTo(uptoKm) <= 0) {
                        ratePerKm = tierRate;
                        break;
                    }
                }
            }
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
        if (rate.getLabourHourlyRate() == null || labourHours == null) {
            return BigDecimal.ZERO;
        }
        return labourHours.multiply(rate.getLabourHourlyRate()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCraneCharge(Rate rate, BigDecimal craneHours) {
        if (rate.getCraneHourlyRate() == null || craneHours == null) {
            return BigDecimal.ZERO;
        }
        return craneHours.multiply(rate.getCraneHourlyRate()).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Estimate labour hours based on commodity and distance
     */
    private BigDecimal estimateLabourHours(Trip trip) {
        // Default: 2 hours
        BigDecimal defaultHours = new BigDecimal("2.0");
        
        // Check commodity type for multiplier
        if (trip.getCommodityType() != null) {
            // Could map commodity to labour hours
            String commodity = trip.getCommodityType().toLowerCase();
            if (commodity.contains("transformer")) return new BigDecimal("4.0");
            if (commodity.contains("drum")) return new BigDecimal("2.5");
            if (commodity.contains("pallet")) return new BigDecimal("2.0");
            if (commodity.contains("chemical")) return new BigDecimal("3.5");
        }
        
        // Distance factor (longer trips need more labour)
        if (trip.getCalculatedDistanceKm() != null) {
            BigDecimal distance = trip.getCalculatedDistanceKm();
            if (distance.compareTo(new BigDecimal("500")) > 0) {
                return new BigDecimal("3.0");
            }
        }
        
        return defaultHours;
    }

    /**
     * Calculate number of days for daily rate
     */
    private Integer calculateTripDays(Trip trip) {
        if (trip.getPlannedStartDate() != null && trip.getPlannedEndDate() != null) {
            long days = Duration.between(
                trip.getPlannedStartDate(),
                trip.getPlannedEndDate()
            ).toDays();
            return Math.max(1, (int) days);
        }
        return 1;
    }

    /**
     * Get applied sliding tier for audit
     */
    private Map<String, Object> getSlidingTier(Rate rate, BigDecimal distance) {
        if (rate.getSlidingScale() == null) return null;
        // Return the tier that was applied
        return null;
    }

    /**
     * Update load billing
     */
    @Transactional
    public LoadBilling updateLoadBilling(String loadId, Long userId) {
        log.info("📦 Updating billing for Load: {}", loadId);

        Load load = loadRepository.findByLoadNumber(loadId)
            .orElseThrow(() -> new RuntimeException("Load not found: " + loadId));

        // Get all trips for this load
        List<Trip> trips = tripRepository.findByLoadId(loadId);
        
        // Get all trip billings
        List<TripBilling> billings = tripBillingRepository.findByTrip_LoadId(loadId);

        LoadBilling loadBilling = loadBillingRepository.findByLoadId(loadId)
            .orElse(LoadBilling.builder().loadId(loadId).build());

        // Aggregate metrics
        loadBilling.setCustomerId(load.getCustomerId());
        loadBilling.setTotalTrips(trips.size());
        
        loadBilling.setTotalDistanceKm(trips.stream()
            .map(Trip::getCalculatedDistanceKm)
            .filter(d -> d != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

        loadBilling.setTotalTonnage(trips.stream()
            .map(Trip::getCargoWeight)
            .filter(w -> w != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add));

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
        loadBilling.setVat(subtotal.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP));
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
            // Delete existing billing
            tripBillingRepository.deleteByTripId(trip.getId());
            // Recalculate
            calculateTripBilling(trip.getId(), userId);
        }

        return updateLoadBilling(loadId, userId);
    }
}
