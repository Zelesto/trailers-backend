package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.TripAggregateKpiDTO;
import com.pgsa.trailers.repository.TripAggregateKpiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripAggregateKpiQueryService {

    private final TripAggregateKpiRepository repository;

    public TripAggregateKpiDTO getAggregateKpis(LocalDate from, LocalDate to) {
        validate(from, to);
        return repository.getAggregateKpis(from, to);
    }

    private void validate(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Date range is required");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From date cannot be after To date");
        }
    }
}
