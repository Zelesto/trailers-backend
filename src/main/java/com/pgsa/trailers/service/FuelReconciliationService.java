package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.FuelReconciliationDTO;
import com.pgsa.trailers.repository.FuelReconciliationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FuelReconciliationService {

    private final FuelReconciliationRepository repository;

    public FuelReconciliationService(FuelReconciliationRepository repository) {
        this.repository = repository;
    }

    public List<FuelReconciliationDTO> reconcileFuel(
            LocalDateTime from,
            LocalDateTime to
    ) {
        List<Object[]> results = repository.reconcileFuelRaw(from, to);

        return results.stream()
                .map(row -> new FuelReconciliationDTO(
                        (String) row[0], // accountName
                        row[1] != null ? BigDecimal.valueOf(((Number) row[1]).doubleValue()) : BigDecimal.ZERO,
                        row[2] != null ? BigDecimal.valueOf(((Number) row[2]).doubleValue()) : BigDecimal.ZERO,
                        row[3] != null ? BigDecimal.valueOf(((Number) row[3]).doubleValue()) : BigDecimal.ZERO
                ))
                .collect(Collectors.toList());
    }

    public FuelReconciliationDTO getReconciliationByAccountName(
            LocalDateTime from,
            LocalDateTime to,
            String accountName
    ) {
        if (accountName == null || accountName.isBlank()) {
            return null;
        }

        return reconcileFuel(from, to).stream()
                .filter(dto -> accountName.equalsIgnoreCase(dto.accountName()))
                .findFirst()
                .orElse(null);
    }
}