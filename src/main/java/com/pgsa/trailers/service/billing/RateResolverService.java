package com.pgsa.trailers.service.billing;

import com.pgsa.trailers.entity.billing.Rate;
import com.pgsa.trailers.entity.billing.RateSchedule;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.billing.RateRepository;
import com.pgsa.trailers.repository.billing.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateResolverService {

    private final RateRepository rateRepository;
    private final HolidayRepository holidayRepository;

    /**
     * Resolve the best matching rate for a trip.
     * <p>
     * ✅ Guarantees the returned {@link Rate} is <b>persisted (managed)</b>, so it
     *    can be safely assigned to a {@code TripBilling} without triggering
     *    {@code TransientPropertyValueException} on the next auto-flush.
     */
    public Rate resolveRate(Trip trip) {
        log.info("🔍 Resolving rate for Trip: {}", trip.getTripNumber());

        LocalDate tripDate = trip.getPlannedStartDate() != null
                ? trip.getPlannedStartDate().toLocalDate()
                : LocalDate.now();

        LocalTime tripTime = trip.getPlannedStartDate() != null
                ? trip.getPlannedStartDate().toLocalTime()
                : LocalTime.now();

        String vehicleType = trip.getVehicle() != null
                ? trip.getVehicle().getVehicleType()
                : "TRUCK";

        String commodity = trip.getCommodityType();
        String destination = trip.getDestinationLocation();

        List<Rate> candidates = rateRepository.findBestMatchingRates(
                trip.getCustomerId(),
                vehicleType,
                destination,
                commodity,
                tripDate
        );

        log.debug("Found {} candidate rates", candidates.size());

        List<Rate> scheduledRates = candidates.stream()
                .filter(rate -> matchesSchedule(rate, tripDate, tripTime))
                .toList();

        if (scheduledRates.isEmpty()) {
            log.warn("⚠️ No matching rate found for Trip: {}, using default",
                    trip.getTripNumber());
            // ✅ Persisted (managed) — never transient
            return getOrCreateDefaultRate();
        }

        scheduledRates.sort((r1, r2) -> {
            boolean r1HasCustomer = r1.getCustomer() != null;
            boolean r2HasCustomer = r2.getCustomer() != null;
            if (r1HasCustomer && !r2HasCustomer) return -1;
            if (!r1HasCustomer && r2HasCustomer) return 1;
            return r2.getPriority().compareTo(r1.getPriority());
        });

        Rate bestRate = scheduledRates.get(0);
        log.info("✅ Resolved rate {} for Trip: {}", bestRate.getId(), trip.getTripNumber());
        return bestRate;
    }

    /**
     * ✅ Return a persisted default Rate. Created once, then reused.
     * <p>
     * Must be called within a read-write transaction (or let the transactional
     * {@code save()} create one). Called from {@code BillingCalculatorService},
     * which is {@code @Transactional}, so it joins that transaction and rolls
     * back cleanly on failure.
     */
   @Transactional
public Rate getOrCreateDefaultRate() {
    return rateRepository.findAll().stream()
            .filter(r -> r.getCustomer() == null)     // ✅ any customer-less rate is a candidate
            .min(Comparator.comparing(Rate::getId))   // ✅ deterministic — picks rate #1
            .orElseGet(() -> {
                log.info("🆕 No default rate exists — creating one");
                return rateRepository.save(buildDefaultRate());
            });
}

    private boolean matchesSchedule(Rate rate, LocalDate tripDate, LocalTime tripTime) {
        RateSchedule schedule = rate.getSchedule();
        if (schedule == null) return true; // Always applies
        return switch (schedule.getApplyOn()) {
            case "WEEKDAY" -> tripDate.getDayOfWeek().getValue() <= 5;
            case "WEEKEND" -> tripDate.getDayOfWeek().getValue() >= 6;
            case "OVERNIGHT" -> tripTime.isBefore(LocalTime.of(6, 0))
                    || tripTime.isAfter(LocalTime.of(22, 0));
            case "HOLIDAY" -> holidayRepository.existsByHolidayDate(tripDate);
            case "SPECIFIC_DATE" -> tripDate.equals(schedule.getSpecificDate());
            default -> true;
        };
    }

private Rate buildDefaultRate() {
    LocalDate today = LocalDate.now();

    Rate r = new Rate();
    r.setPerKm(new BigDecimal("18.50"));
    r.setPerTon(null);
    r.setPerDay(null);
    r.setLabourHourlyRate(null);
    r.setCraneHourlyRate(new BigDecimal("750.00"));
    r.setFixedAmount(BigDecimal.ZERO);
    r.setPriority(-100);               // ✅ matches your existing defaults
    r.setValidFrom(today);             // ✅ REQUIRED — DB column is NOT NULL
    r.setValidTo(today.plusYears(10)); // ✅ open-ended default
    return r;
}
}
