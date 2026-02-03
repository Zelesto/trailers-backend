package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.TripAggregateKpiDTO;
import com.pgsa.trailers.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripAnalyticsService {

    private final TripKpiQueryService tripKpiQueryService;
    private final TripAggregateKpiQueryService aggregateKpiQueryService;

    public List<TripKpiDTO> getTripKpis(LocalDate from, LocalDate to) {
        return tripKpiQueryService.getTripKpis(from, to);
    }

    public TripAggregateKpiDTO getAggregateKpis(LocalDate from, LocalDate to) {
        return aggregateKpiQueryService.getAggregateKpis(from, to);
    }
}
