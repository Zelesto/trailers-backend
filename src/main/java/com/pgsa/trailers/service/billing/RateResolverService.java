package com.pgsa.trailers.service.billing;

import com.pgsa.trailers.entity.billing.Rate;
import com.pgsa.trailers.entity.billing.RateSchedule;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.billing.RateRepository;
import com.pgsa.trailers.repository.billing.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateResolverService {

    private final RateRepository rateRepository;
    private final HolidayRepository holidayRepository;

    /**
     * Resolve the best matching rate for a trip
     */
    public Rate resolveRate(Trip trip) {
        log.info("🔍 Resolving rate for Trip: {}", trip.getTripNumber());

        LocalDate tripDate = trip.getPlannedStartDate() != null 
            ? trip.getPlannedStartDate().toLocalDate() 
            : LocalDate.now();

        LocalTime tripTime = trip.getPlannedStartDate() != null 
            ? trip.getPlannedStartDate().toLocalTime() 
            : LocalTime.now();

        // Get vehicle type string from vehicle
        String vehicleType = trip.getVehicle() != null 
            ? trip.getVehicle().getVehicleType() 
            : "TRUCK";

        // Get commodity from trip
        String commodity = trip.getCommodityType();

        // Get destination (for pattern matching)
        String destination = trip.getDestinationLocation();

        // 1. Find candidate rates with best match
        List<Rate> candidates = rateRepository.findBestMatchingRates(
            trip.getCustomerId(),
            vehicleType,
            destination,
            commodity,
            tripDate
        );

        log.debug("Found {} candidate rates", candidates.size());

        // 2. Filter by schedule
        List<Rate> scheduledRates = candidates.stream()
            .filter(rate -> matchesSchedule(rate, tripDate, tripTime))
            .toList();

        if (scheduledRates.isEmpty()) {
            log.warn("⚠️ No matching rate found for Trip: {}, using default", trip.getTripNumber());
            return createDefaultRate();
        }

        // 3. Sort by priority (customer-specific first)
        scheduledRates.sort((r1, r2) -> {
            // Customer-specific rates beat defaults
            boolean r1HasCustomer = r1.getCustomer() != null;
            boolean r2HasCustomer = r2.getCustomer() != null;
            
            if (r1HasCustomer && !r2HasCustomer) return -1;
            if (!r1HasCustomer && r2HasCustomer) return 1;
            
            // Then by priority (higher first)
            return r2.getPriority().compareTo(r1.getPriority());
        });

        Rate bestRate = scheduledRates.get(0);
        log.info("✅ Resolved rate {} for Trip: {}", bestRate.getId(), trip.getTripNumber());
        
        return bestRate;
    }

    /**
     * Check if rate schedule matches trip date/time
     */
    private boolean matchesSchedule(Rate rate, LocalDate tripDate, LocalTime tripTime) {
        RateSchedule schedule = rate.getSchedule();
        
        if (schedule == null) {
            return true; // Always applies
        }

        return switch (schedule.getApplyOn()) {
            case "WEEKDAY" -> tripDate.getDayOfWeek().getValue() <= 5;
            case "WEEKEND" -> tripDate.getDayOfWeek().getValue() >= 6;
            case "OVERNIGHT" -> tripTime.isBefore(LocalTime.of(6, 0)) || 
                               tripTime.isAfter(LocalTime.of(22, 0));
            case "HOLIDAY" -> holidayRepository.existsByHolidayDate(tripDate);
            case "SPECIFIC_DATE" -> tripDate.equals(schedule.getSpecificDate());
            default -> true;
        };
    }

    /**
     * Check if destination matches pattern
     */
    private boolean destinationMatches(String destination, String pattern) {
        if (pattern == null) return true;
        if (destination == null) return false;
        return destination.toUpperCase().contains(pattern.toUpperCase());
    }

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
}
