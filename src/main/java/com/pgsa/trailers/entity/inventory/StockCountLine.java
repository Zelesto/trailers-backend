package com.pgsa.trailers.entity.inventory;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "stock_count_lines")
public class StockCountLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------------- RELATIONSHIPS ----------------

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_count_id", nullable = false)
    private StockCount stockCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    // ---------------- QUANTITIES ----------------

    @Column(name = "system_qty", precision = 18, scale = 4, nullable = false)
    private BigDecimal systemQty = BigDecimal.ZERO;

    @Column(name = "counted_qty", precision = 18, scale = 4, nullable = false)
    private BigDecimal countedQty = BigDecimal.ZERO;

    @Column(name = "variance", precision = 18, scale = 4, nullable = false)
    private BigDecimal variance = BigDecimal.ZERO;

    // ---------------- CONSTRUCTORS ----------------

    public StockCountLine() { }

    public StockCountLine(InventoryItem item, BigDecimal systemQty, BigDecimal countedQty) {
        this.item = item;
        this.systemQty = systemQty != null ? systemQty : BigDecimal.ZERO;
        this.countedQty = countedQty != null ? countedQty : BigDecimal.ZERO;
        calculateVariance();
    }

    // ---------------- BUSINESS LOGIC ----------------

    @PrePersist
    @PreUpdate
    public void calculateVariance() {
        if (systemQty != null && countedQty != null) {
            this.variance = countedQty.subtract(systemQty);
        }
    }

    // ---------------- GETTERS & SETTERS ----------------

    public Long getId() { return id; }

    public StockCount getStockCount() { return stockCount; }

    public void setStockCount(StockCount stockCount) {
        this.stockCount = stockCount;
    }

    public InventoryItem getItem() { return item; }

    public void setItem(InventoryItem item) { this.item = item; }

    public BigDecimal getSystemQty() { return systemQty; }

    public void setSystemQty(BigDecimal systemQty) {
        this.systemQty = systemQty != null ? systemQty : BigDecimal.ZERO;
        calculateVariance();
    }

    public BigDecimal getCountedQty() { return countedQty; }

    public void setCountedQty(BigDecimal countedQty) {
        this.countedQty = countedQty != null ? countedQty : BigDecimal.ZERO;
        calculateVariance();
    }

    public BigDecimal getVariance() { return variance; }
}