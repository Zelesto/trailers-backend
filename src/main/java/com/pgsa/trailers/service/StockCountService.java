package com.pgsa.trailers.service;

import com.pgsa.trailers.entity.inventory.StockCount;
import com.pgsa.trailers.entity.inventory.StockCountLine;
import com.pgsa.trailers.entity.inventory.StockMovement;
import com.pgsa.trailers.enums.StockMovementType;
import com.pgsa.trailers.enums.StockCountStatus;
import com.pgsa.trailers.repository.StockCountRepository;
import com.pgsa.trailers.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pgsa.trailers.dto.InventoryVarianceDTO;
import com.pgsa.trailers.entity.inventory.InventoryItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockCountService {

    private final StockCountRepository countRepo;
    private final StockMovementRepository movementRepo;

    public StockCountService(
            StockCountRepository countRepo,
            StockMovementRepository movementRepo
    ) {
        this.countRepo = countRepo;
        this.movementRepo = movementRepo;
    }

    /**
     * Records a stock movement in the system.
     */
    public void recordStockMovement(StockMovement movement) {
        if (movement == null) throw new IllegalArgumentException("Movement cannot be null");
        if (movement.getQuantity() == null || movement.getQuantity().compareTo(BigDecimal.ZERO) == 0) return;
        movementRepo.save(movement);
    }

    /**
     * Returns a list of variance reports for a given stock count.
     */
    public List<VarianceReport> getVarianceReport(Long countId) {
        StockCount count = countRepo.findById(countId)
                .orElseThrow(() -> new IllegalArgumentException("StockCount not found for ID " + countId));

        List<VarianceReport> report = new ArrayList<>();
        for (StockCountLine line : count.getLines()) {
            BigDecimal variance = line.getVariance();
            if (variance.compareTo(BigDecimal.ZERO) != 0) {
                VarianceReport vr = new VarianceReport();
                vr.setItem(line.getItem());
                vr.setExpectedQty(line.getSystemQty());
                vr.setCountedQty(line.getCountedQty());
                vr.setVariance(variance);
                report.add(vr);
            }
        }
        return report;
    }

    /**
     * Posts a stock count: creates StockMovements for any line with variance and updates the count status.
     */
    @Transactional
    public void postStockCount(Long countId) {
        StockCount count = countRepo.findById(countId)
                .orElseThrow(() -> new IllegalArgumentException("StockCount not found for ID " + countId));

        if (count.getStatus() == StockCountStatus.POSTED) {
            throw new IllegalStateException("Stock count already posted");
        }

        for (StockCountLine line : count.getLines()) {
            BigDecimal variance = line.getVariance();
            if (variance.compareTo(BigDecimal.ZERO) != 0) {
                StockMovement movement = new StockMovement();
                movement.setItem(line.getItem());
                movement.setLocation(count.getLocation());
                movement.setMovementType(StockMovementType.ADJUSTMENT);
                movement.setQuantity(variance);
                movement.setReferenceType("STOCK_COUNT");
                movement.setReferenceId(countId);

                recordStockMovement(movement);
            }
        }

        count.setStatus(StockCountStatus.POSTED);
        countRepo.save(count);
    }

    /**
     * Returns a shrinkage report for a given stock count.
     */
    public InventoryVarianceDTO getShrinkageReport(Long countId) {
        StockCount count = countRepo.findById(countId)
                .orElseThrow(() -> new IllegalArgumentException("StockCount not found for ID " + countId));

        // For simplicity, we'll aggregate the variance of all lines in the count
        BigDecimal totalSystemQty = count.getLines().stream()
                .map(StockCountLine::getSystemQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCountedQty = count.getLines().stream()
                .map(StockCountLine::getCountedQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVariance = count.getLines().stream()
                .map(StockCountLine::getVariance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new InventoryVarianceDTO(
                "Total Shrinkage for Count #" + countId,
                totalSystemQty,
                totalCountedQty,
                totalVariance
        );
    }

    /**
     * DTO for variance reporting.
     */
    public static class VarianceReport {
        private Object item; // Replace Object with InventoryItem if desired
        private BigDecimal expectedQty;
        private BigDecimal countedQty;
        private BigDecimal variance;

        public Object getItem() { return item; }
        public void setItem(Object item) { this.item = item; }
        public BigDecimal getExpectedQty() { return expectedQty; }
        public void setExpectedQty(BigDecimal expectedQty) { this.expectedQty = expectedQty; }
        public BigDecimal getCountedQty() { return countedQty; }
        public void setCountedQty(BigDecimal countedQty) { this.countedQty = countedQty; }
        public BigDecimal getVariance() { return variance; }
        public void setVariance(BigDecimal variance) { this.variance = variance; }
    }
}