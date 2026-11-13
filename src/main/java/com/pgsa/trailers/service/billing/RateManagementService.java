package com.pgsa.trailers.service.billing;

import com.pgsa.trailers.dto.billing.RateReferenceData;
import com.pgsa.trailers.dto.billing.RateRequest;
import com.pgsa.trailers.dto.billing.RateResponse;
import com.pgsa.trailers.entity.assets.VehicleType;
import com.pgsa.trailers.entity.billing.Rate;
import com.pgsa.trailers.entity.billing.RateSchedule;
import com.pgsa.trailers.entity.ops.Commodity;
import com.pgsa.trailers.entity.ops.Customer;
import com.pgsa.trailers.repository.CustomerRepository;
import com.pgsa.trailers.repository.assets.VehicleTypeRepository;
import com.pgsa.trailers.repository.billing.RateRepository;
import com.pgsa.trailers.repository.billing.RateScheduleRepository;
import com.pgsa.trailers.repository.ops.CommodityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class RateManagementService {

    private final RateRepository rateRepository;
    private final CustomerRepository customerRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final CommodityRepository commodityRepository;
    private final RateScheduleRepository rateScheduleRepository;

    @Transactional(readOnly = true)
    public List<RateResponse> list(boolean includeInactive) {
        return rateRepository.findAll().stream()
                .filter(rate -> includeInactive || Boolean.TRUE.equals(rate.getIsActive()))
                .sorted(Comparator.comparing(Rate::getId).reversed())
                .map(RateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RateReferenceData getReferences() {
        return new RateReferenceData(
                customerRepository.findByIsActiveTrue().stream()
                        .map(customer -> new RateReferenceData.Option(customer.getId(), customer.getName())).toList(),
                vehicleTypeRepository.findByIsActiveTrueOrderByNameAsc().stream()
                        .map(type -> new RateReferenceData.Option(type.getId(), type.getName())).toList(),
                commodityRepository.findByIsActiveTrueOrderByNameAsc().stream()
                        .map(commodity -> new RateReferenceData.Option(commodity.getId(), commodity.getName())).toList(),
                rateScheduleRepository.findByIsActiveTrueOrderByNameAsc().stream()
                        .map(schedule -> new RateReferenceData.Option(schedule.getId(), schedule.getName())).toList());
    }

    @Transactional
    public RateResponse create(RateRequest request) {
        Rate rate = new Rate();
        rate.setIsActive(true);
        apply(rate, request);
        return RateResponse.from(rateRepository.save(rate));
    }

    @Transactional
    public RateResponse update(Long id, RateRequest request) {
        Rate rate = findRate(id);
        apply(rate, request);
        return RateResponse.from(rateRepository.save(rate));
    }

    @Transactional
    public RateResponse setActive(Long id, boolean active) {
        Rate rate = findRate(id);
        rate.setIsActive(active);
        return RateResponse.from(rateRepository.save(rate));
    }

    private Rate findRate(Long id) {
        return rateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rate not found"));
    }

    private void apply(Rate rate, RateRequest request) {
        if (request.validFrom().isAfter(request.validTo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid-to date must be on or after valid-from");
        }
        if (allAmountsAreZero(request)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one rate amount must be greater than zero");
        }

        rate.setCustomer(resolve(request.customerId(), customerRepository::findById, "Customer"));
        rate.setVehicleType(resolve(request.vehicleTypeId(), vehicleTypeRepository::findById, "Vehicle type"));
        rate.setCommodity(resolve(request.commodityId(), commodityRepository::findById, "Commodity"));
        rate.setSchedule(resolve(request.scheduleId(), rateScheduleRepository::findById, "Rate schedule"));
        rate.setDestinationPattern(blankToNull(request.destinationPattern()));
        rate.setFixedAmount(request.fixedAmount() == null ? BigDecimal.ZERO : request.fixedAmount());
        rate.setPerKm(request.perKm());
        rate.setPerTon(request.perTon());
        rate.setPerDay(request.perDay());
        rate.setLabourHourlyRate(request.labourHourlyRate());
        rate.setCraneHourlyRate(request.craneHourlyRate());
        rate.setPriority(request.priority() == null ? 0 : request.priority());
        rate.setValidFrom(request.validFrom());
        rate.setValidTo(request.validTo());
    }

    private boolean allAmountsAreZero(RateRequest request) {
        return isZero(request.fixedAmount())
                && isZero(request.perKm())
                && isZero(request.perTon())
                && isZero(request.perDay())
                && isZero(request.labourHourlyRate())
                && isZero(request.craneHourlyRate());
    }

    private boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private <T> T resolve(Long id, Function<Long, java.util.Optional<T>> lookup, String label) {
        if (id == null) {
            return null;
        }
        return lookup.apply(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " not found"));
    }
}
