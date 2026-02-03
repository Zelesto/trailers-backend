package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.TripKpiDTO;
import com.pgsa.trailers.repository.TripKpiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripKpiQueryService {

    private final TripKpiRepository repository;

    public List<TripKpiDTO> getTripKpis(LocalDate from, LocalDate to) {
        validateDateRange(from, to);
        return repository.findTripKpis(from, to);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Date range is required");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From date cannot be after To date");
        }
    }
}
