package com.pgsa.trailers.entity.inventory;

import com.pgsa.trailers.config.BaseEntity;
import com.pgsa.trailers.enums.StockCountStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "stock_count")
public class StockCount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id")
    private InventoryLocation location;

    @Column(nullable = false)
    private LocalDate countDate;

    private String performedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockCountStatus status = StockCountStatus.DRAFT;

    @OneToMany(
            mappedBy = "stockCount",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<StockCountLine> lines = new ArrayList<>();

    // ---------------- CONSTRUCTORS ----------------

    public StockCount() { }

    public StockCount(Long id) {
        this.setId(id); // from BaseEntity
    }

    // ---------------- HELPER METHODS ----------------

    /**
     * Adds a line to this stock count and sets the back-reference.
     */
    public void addLine(StockCountLine line) {
        lines.add(line);
        line.setStockCount(this);
    }

    /**
     * Removes a line from this stock count and clears the back-reference.
     */
    public void removeLine(StockCountLine line) {
        lines.remove(line);
        line.setStockCount(null);
    }

    /**
     * Convenience method to check if the stock count is posted.
     */
    public boolean isPosted() {
        return status == StockCountStatus.POSTED;
    }
}
