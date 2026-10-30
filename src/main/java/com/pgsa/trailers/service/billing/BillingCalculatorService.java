package com.pgsa.trailers.service.billing;

import com.pgsa.trailers.entity.billing.LoadBilling;
import com.pgsa.trailers.entity.billing.Rate;
import com.pgsa.trailers.entity.billing.TripBilling;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.TripRepository;
import com.pgsa.trailers.repository.billing.LoadBillingRepository;
import com.pgsa.trailers.repository.billing.TripBillingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingCalculatorService {

    private final RateResolverService rateResolverService;
    private final TripBillingRepository tripBillingRepository;
    private final LoadBillingRepository loadBillingRepository;
    private final LoadRepository loadRepository;
    private final TripRepository tripRepository;

    private static final BigDecimal VAT_RATE          = new BigDecimal("0.15");
    private static final BigDecimal TON_CONVERSION    = new BigDecimal("1000");
    private static final BigDecimal CRANE_BASE_CHARGE = new BigDecimal("150.00");

    // ============ MAIN CALCULATION ============

    @Transactional
    public TripBilling calculateTripBilling(Long tripId, Long userId) {
        log.info("💰 Calculating billing for Trip ID: {}", tripId);

        Trip trip = tripRepository.findByIdWithAllRelations(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        TripBilling existing = tripBillingRepository.findByTripId(tripId);
        if (existing != null && "CALCULATED".equals(existing.getStatus())) {
            log.info("Trip {} already has billing calculated", tripId);
            return existing;
        }

        Rate rate = rateResolverService.resolveRate(trip);
        if (rate == null || rate.getId() == null) {
            log.warn("⚠️ Resolver returned a transient rate for trip {}, forcing default", tripId);
            rate = rateResolverService.getOrCreateDefaultRate();
        }

        BigDecimal distance    = getTripDistance(trip);
        BigDecimal tonnage     = getTripTonnage(trip);
        Integer    days        = calculateTripDays(trip);
        BigDecimal labourHours = estimateLabourHours(trip);
        BigDecimal craneHours  = getCraneHours(trip);

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
                .distanceCharge(calculateDistanceCharge(rate, distance))
                .tonnageCharge(calculateTonnageCharge(rate, tonnage))
                .dailyRateCharge(calculateDailyRateCharge(rate, days))
                .labourCharge(calculateLabourCharge(rate, labourHours))
                .craneCharge(calculateCraneCharge(rate, craneHours))
                .fixedSurcharge(rate.getFixedAmount() != null
                        ? rate.getFixedAmount() : BigDecimal.ZERO)
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

        if (trip.getLoadId() != null && !trip.getLoadId().isEmpty()) {
            updateLoadBilling(trip.getLoadId(), userId);
        }
        return saved;
    }

    // ============ TRIP METRICS ============

    private BigDecimal getTripDistance(Trip trip) {
        BigDecimal d = trip.getActualDistanceKm() != null
                && trip.getActualDistanceKm().compareTo(BigDecimal.ZERO) > 0
                ? trip.getActualDistanceKm() : trip.getCalculatedDistanceKm();
        if (d == null || d.compareTo(BigDecimal.ZERO) <= 0) {
            d = trip.getPlannedDistanceKm() != null ? trip.getPlannedDistanceKm() : BigDecimal.ZERO;
        }
        return d;
    }

    private BigDecimal getTripTonnage(Trip trip) {
        BigDecimal kg = trip.getCargoWeight() != null ? trip.getCargoWeight() : BigDecimal.ZERO;
        return kg.divide(TON_CONVERSION, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal getCraneHours(Trip trip) {
        return Boolean.TRUE.equals(trip.getCraneUsed()) ? BigDecimal.ONE : BigDecimal.ZERO;
    }

    private Integer calculateTripDays(Trip trip) {
        if (trip.getActualStartDate() != null && trip.getActualEndDate() != null) {
            long d = Duration.between(trip.getActualStartDate(), trip.getActualEndDate()).toDays();
            return Math.max(1, (int) d);
        }
        if (trip.getPlannedStartDate() != null && trip.getPlannedEndDate() != null) {
            long d = Duration.between(trip.getPlannedStartDate(), trip.getPlannedEndDate()).toDays();
            return Math.max(1, (int) d);
        }
        if (trip.getEstimatedDurationHours() != null) {
            int d = trip.getEstimatedDurationHours()
                    .divide(new BigDecimal("24"), 0, RoundingMode.HALF_UP).intValue();
            return Math.max(1, d);
        }
        return 1;
    }

    private BigDecimal estimateLabourHours(Trip trip) {
        if (trip.getCommodityType() != null) {
            String c = trip.getCommodityType().toLowerCase();
            if (c.contains("transformer") || c.contains("heavy"))   return new BigDecimal("4.0");
            if (c.contains("chemical")   || c.contains("hazardous")) return new BigDecimal("3.5");
        }
        BigDecimal dist = trip.getCalculatedDistanceKm() != null
                ? trip.getCalculatedDistanceKm() : trip.getPlannedDistanceKm();
        if (dist != null) {
            if (dist.compareTo(new BigDecimal("1000")) > 0) return new BigDecimal("4.0");
            if (dist.compareTo(new BigDecimal("500"))  > 0) return new BigDecimal("3.0");
            if (dist.compareTo(new BigDecimal("200"))  > 0) return new BigDecimal("2.5");
        }
        return new BigDecimal("2.0");
    }

    // ============ CHARGE CALCULATIONS ============

    private BigDecimal calculateDistanceCharge(Rate rate, BigDecimal distance) {
        if (rate.getPerKm() == null || distance == null
                || distance.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        BigDecimal perKm = rate.getPerKm();
        if (distance.compareTo(new BigDecimal("500")) > 0)      perKm = perKm.multiply(new BigDecimal("0.90"));
        else if (distance.compareTo(new BigDecimal("200")) > 0) perKm = perKm.multiply(new BigDecimal("0.95"));
        return distance.multiply(perKm).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTonnageCharge(Rate rate, BigDecimal tonnage) {
        if (rate.getPerTon() == null || tonnage == null
                || tonnage.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return tonnage.multiply(rate.getPerTon()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDailyRateCharge(Rate rate, Integer days) {
        if (rate.getPerDay() == null || days == null || days <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(days).multiply(rate.getPerDay()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateLabourCharge(Rate rate, BigDecimal hours) {
        if (rate.getLabourHourlyRate() == null || hours == null
                || hours.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return hours.multiply(rate.getLabourHourlyRate()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCraneCharge(Rate rate, BigDecimal hours) {
        if (hours == null || hours.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        BigDecimal craneRate = rate.getCraneHourlyRate() != null
                ? rate.getCraneHourlyRate() : CRANE_BASE_CHARGE;
        return hours.multiply(craneRate).setScale(2, RoundingMode.HALF_UP);
    }

    // ============ LOAD BILLING ============

    @Transactional
    public LoadBilling updateLoadBilling(String loadId, Long userId) {
        log.info("📦 Updating billing for Load: {}", loadId);

        Load load = resolveLoad(loadId);
        String canonicalLoadNumber = load.getLoadNumber();
        log.info("  → using loadNumber: {}", canonicalLoadNumber);

        List<Trip> trips = tripRepository.findByLoadId(canonicalLoadNumber);
        List<TripBilling> billings = tripBillingRepository.findByTrip_LoadId(canonicalLoadNumber);

        LoadBilling lb = loadBillingRepository.findByLoadId(canonicalLoadNumber)
                .orElse(LoadBilling.builder().loadId(canonicalLoadNumber).build());

        lb.setCustomerId(load.getCustomerId());
        lb.setTotalTrips(trips.size());
        lb.setTotalDistanceKm(trips.stream()
                .map(t -> t.getCalculatedDistanceKm() != null
                        ? t.getCalculatedDistanceKm() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        lb.setTotalTonnage(trips.stream()
                .map(t -> t.getCargoWeight() != null
                        ? t.getCargoWeight().divide(TON_CONVERSION, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        lb.setTotalDistanceCharge(sum(billings, TripBilling::getDistanceCharge));
        lb.setTotalTonnageCharge(sum(billings, TripBilling::getTonnageCharge));
        lb.setTotalDailyCharge(sum(billings, TripBilling::getDailyRateCharge));
        lb.setTotalLabourCharge(sum(billings, TripBilling::getLabourCharge));
        lb.setTotalCraneCharge(sum(billings, TripBilling::getCraneCharge));
        lb.setTotalFixedSurcharge(sum(billings, TripBilling::getFixedSurcharge));

        BigDecimal subtotal = BigDecimal.ZERO
                .add(lb.getTotalDistanceCharge())
                .add(lb.getTotalTonnageCharge())
                .add(lb.getTotalDailyCharge())
                .add(lb.getTotalLabourCharge())
                .add(lb.getTotalCraneCharge())
                .add(lb.getTotalFixedSurcharge());

        lb.setSubtotal(subtotal);
        lb.setVat(subtotal.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP));
        lb.setTotal(subtotal.add(lb.getVat()));

        if (lb.getStatus() == null) lb.setStatus("CALCULATED");
        lb.setCalculatedAt(LocalDateTime.now());
        lb.setCalculatedBy(userId);

        return loadBillingRepository.save(lb);
    }

    private static BigDecimal sum(List<TripBilling> list,
                                  Function<TripBilling, BigDecimal> getter) {
        return list.stream().map(getter).filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Load resolveLoad(String loadIdOrNumber) {
        if (loadIdOrNumber == null || loadIdOrNumber.isBlank()) {
            throw new IllegalArgumentException("loadId is required");
        }
        var byNumber = loadRepository.findByLoadNumber(loadIdOrNumber);
        if (byNumber.isPresent()) {
            log.info("  → resolved load by loadNumber: {}", loadIdOrNumber);
            return byNumber.get();
        }
        try {
            Long numericId = Long.parseLong(loadIdOrNumber);
            Load load = loadRepository.findById(numericId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Load not found by loadNumber '" + loadIdOrNumber
                                    + "' or id " + numericId));
            log.info("  → resolved load by numeric id: {} → loadNumber: {}",
                    numericId, load.getLoadNumber());
            return load;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Load not found: " + loadIdOrNumber);
        }
    }

    // ============ RECALCULATION ============

    @Transactional
    public LoadBilling recalculateLoadBilling(String loadId, Long userId) {
        log.info("🔄 Recalculating billing for Load: {}", loadId);

        Load load = resolveLoad(loadId);
        String canonicalLoadNumber = load.getLoadNumber();
        log.info("  → canonical loadNumber: {}", canonicalLoadNumber);

        List<Trip> trips = tripRepository.findByLoadId(canonicalLoadNumber);
        for (Trip trip : trips) {
            TripBilling existing = tripBillingRepository.findByTripId(trip.getId());
            if (existing != null) tripBillingRepository.delete(existing);
            calculateTripBilling(trip.getId(), userId);
        }
        return updateLoadBilling(canonicalLoadNumber, userId);
    }

    // ============ QUERIES ============

    public LoadBilling getLoadBilling(String loadId) {
        var byLoadId = loadBillingRepository.findByLoadId(loadId);
        if (byLoadId.isPresent()) return byLoadId.get();
        try {
            Long numericId = Long.parseLong(loadId);
            return loadBillingRepository.findById(numericId)
                    .orElseThrow(() -> new IllegalArgumentException("Load billing not found: " + loadId));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Load billing not found: " + loadId);
        }
    }

    public TripBilling getTripBilling(Long tripId) {
        TripBilling b = tripBillingRepository.findByTripId(tripId);
        if (b == null) throw new IllegalArgumentException("Trip billing not found: " + tripId);
        return b;
    }

    public List<LoadBilling> getBillableLoads() {
        try {
            List<LoadBilling> billable = loadBillingRepository.findBillableLoads();
            if (billable != null && !billable.isEmpty()) return billable;

            List<LoadBilling> generated = new ArrayList<>();
            for (Load load : loadRepository.findAll()) {
                if (load == null || load.getLoadNumber() == null
                        || load.getLoadNumber().isBlank()) continue;
                try {
                    List<Trip> trips = tripRepository.findByLoadId(load.getLoadNumber());
                    if (trips == null || trips.isEmpty()) continue;
                    LoadBilling lb = loadBillingRepository
                            .findByLoadId(load.getLoadNumber()).orElse(null);
                    if (lb == null) lb = recalculateLoadBilling(load.getLoadNumber(), 0L);
                    if (lb != null && lb.getInvoiceId() == null
                            && ("CALCULATED".equalsIgnoreCase(lb.getStatus())
                                || "DRAFT".equalsIgnoreCase(lb.getStatus()))) {
                        generated.add(lb);
                    }
                } catch (Exception e) {
                    log.warn("Failed to backfill billable load {}: {}",
                            load.getLoadNumber(), e.getMessage());
                }
            }
            return generated;
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
