package com.pgsa.trailers.service.billing;

import com.pgsa.trailers.entity.billing.Rate;
import com.pgsa.trailers.entity.billing.RateSchedule;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.billing.HolidayRepository;
import com.pgsa.trailers.repository.billing.RateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateResolverService {

    private final RateRepository rateRepository;
    private final HolidayRepository holidayRepository;

    /**
     * Resolve the best matching rate for a trip.
     * ✅ Guarantees the returned {@link Rate} is <b>persisted (managed)</b>.
     */
    public Rate resolveRate(Trip trip) {
        log.info("🔍 Resolving rate for Trip: {}", trip.getTripNumber());

        LocalDate tripDate = trip.getPlannedStartDate() != null
                ? trip.getPlannedStartDate().toLocalDate()
                : LocalDate.now();

        LocalTime tripTime = trip.getPlannedStartDate() != null
                ? trip.getPlannedStartDate().toLocalTime()
                : LocalTime.now();

        Long customerId = trip.getCustomerId();

        List<Rate> allValid = rateRepository.findAllValidAtDate(tripDate);
        log.debug("Loaded {} rates valid on {}", allValid.size(), tripDate);

        List<Rate> matching = allValid.stream()
                .filter(r -> matchesCustomer(r, customerId))
                .filter(r -> matchesSchedule(r, tripDate, tripTime))
                .toList();

        if (matching.isEmpty()) {
            log.warn("⚠️ No matching rate found for Trip: {}, using default",
                    trip.getTripNumber());
            return getOrCreateDefaultRate();
        }

        Rate best = matching.stream()
                .min(Comparator
                        .comparing((Rate r) -> r.getCustomer() == null ? 1 : 0)
                        .thenComparing(r -> r.getPriority() == null
                                ? Integer.MIN_VALUE
                                : -r.getPriority()))
                .orElseThrow();

        log.info("✅ Resolved rate {} for Trip: {}", best.getId(), trip.getTripNumber());
        return best;
    }

    /**
     * ✅ Return a persisted default rate. Any customer-agnostic rate is a
     * candidate (matches production rows with {@code priority = -100}).
     * Picks lowest {@code id} deterministically.
     */
    @Transactional
    public Rate getOrCreateDefaultRate() {
        return rateRepository.findAll().stream()
                .filter(r -> r.getCustomer() == null)
                .min(Comparator.comparing(Rate::getId))
                .orElseGet(() -> {
                    log.info("🆕 No default rate exists — creating one");
                    return rateRepository.save(buildDefaultRate());
                });
    }

    // ---------------------------------------------------------------------

    private boolean matchesCustomer(Rate rate, Long customerId) {
        if (rate.getCustomer() == null) return true;   // generic rate
        if (customerId == null) return false;
        return customerId.equals(rate.getCustomer().getId());
    }

    private boolean matchesSchedule(Rate rate, LocalDate tripDate, LocalTime tripTime) {
        RateSchedule schedule = rate.getSchedule();
        if (schedule == null) return true;
        String applyOn = schedule.getApplyOn();
        if (applyOn == null) return true;   // ✅ null-safe switch

        return switch (applyOn) {
            case "WEEKDAY" -> tripDate.getDayOfWeek().getValue() <= 5;
            case "WEEKEND" -> tripDate.getDayOfWeek().getValue() >= 6;
            case "OVERNIGHT" -> tripTime.isBefore(LocalTime.of(6, 0))
                    || tripTime.isAfter(LocalTime.of(22, 0));
            case "HOLIDAY" -> holidayRepository.existsByHolidayDate(tripDate);
            case "SPECIFIC_DATE" -> tripDate.equals(schedule.getSpecificDate());
            default -> true;
        };
    }

    /**
     * ✅ Mirrors production defaults: per_km=18.50, crane=750, priority=-100.
     * <b>validFrom MUST be set</b> — the {@code rates.valid_from} column is NOT NULL.
     */
    private Rate buildDefaultRate() {
        Rate r = new Rate();
        r.setPerKm(new BigDecimal("18.50"));
        r.setPerTon(null);
        r.setPerDay(null);
        r.setLabourHourlyRate(null);
        r.setCraneHourlyRate(new BigDecimal("750.00"));
        r.setFixedAmount(BigDecimal.ZERO);
        r.setPriority(-100);
        r.setValidFrom(LocalDate.now());
        r.setValidTo(LocalDate.of(2099, 12, 31));
        return r;
    }
}
