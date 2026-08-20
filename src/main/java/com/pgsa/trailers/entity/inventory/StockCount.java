package com.pgsa.trailers.entity.inventory;

import com.pgsa.trailers.config.BaseEntity;
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

    // ============================================================
    // CONSTANTS FOR STATUS VALUES (from enum_master table)
    // ============================================================
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_POSTED = "POSTED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id")
    private InventoryLocation location;

    @Column(nullable = false)
    private LocalDate countDate;

    private String performedBy;

    @Column(nullable = false)
    private String status = STATUS_DRAFT;

    @OneToMany(
            mappedBy = "stockCount",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<StockCountLine> lines = new ArrayList<>();

    // ---------------- CONSTRUCTORS ----------------

    public StockCount() { }

    public StockCount(Long id) {
        this.setId(id);
    }

    // ---------------- GETTERS AND SETTERS ----------------

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
        return STATUS_POSTED.equals(status);
    }

    /**
     * Convenience method to check if the stock count is in draft.
     */
    public boolean isDraft() {
        return STATUS_DRAFT.equals(status);
    }

    /**
     * Convenience method to check if the stock count is in progress.
     */
    public boolean isInProgress() {
        return STATUS_IN_PROGRESS.equals(status);
    }

    /**
     * Convenience method to check if the stock count is completed.
     */
    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    /**
     * Convenience method to check if the stock count is cancelled.
     */
    public boolean isCancelled() {
        return STATUS_CANCELLED.equals(status);
    }

    /**
     * Check if the stock count can be edited.
     */
    public boolean canBeEdited() {
        return STATUS_DRAFT.equals(status) || STATUS_IN_PROGRESS.equals(status);
    }

    /**
     * Check if the stock count can be posted.
     */
    public boolean canBePosted() {
        return STATUS_COMPLETED.equals(status) || STATUS_IN_PROGRESS.equals(status);
    }

    /**
     * Check if the stock count can be cancelled.
     */
    public boolean canBeCancelled() {
        return !STATUS_POSTED.equals(status);
    }

    /**
     * Get status display name.
     */
    public String getStatusDisplay() {
        if (status == null) {
            return "UNKNOWN";
        }
        switch (status) {
            case STATUS_DRAFT:
                return "Draft";
            case STATUS_IN_PROGRESS:
                return "In Progress";
            case STATUS_COMPLETED:
                return "Completed";
            case STATUS_POSTED:
                return "Posted";
            case STATUS_CANCELLED:
                return "Cancelled";
            default:
                return status;
        }
    }

    // ---------------- LIFECYCLE CALLBACKS ----------------

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = STATUS_DRAFT;
        }
        if (countDate == null) {
            countDate = LocalDate.now();
        }
    }
}
