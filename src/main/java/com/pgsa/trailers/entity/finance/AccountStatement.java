package com.pgsa.trailers.entity.finance;

import com.pgsa.trailers.config.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_statement")
public class AccountStatement extends BaseEntity {

    /**
     * Proper FK mapping
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "statement_date", nullable = false)
    private LocalDateTime statementDate;

    @Column(name = "opening_balance")
    private BigDecimal openingBalance;

    @Column(name = "closing_balance")
    private BigDecimal closingBalance;

    @Column(name = "total_debits")
    private BigDecimal totalDebits;

    @Column(name = "total_credits")
    private BigDecimal totalCredits;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "recon_date")
    private LocalDateTime reconDate;

    @Column(name = "created_by")
    private String createdBy;

    // ========== GETTERS ==========

    public Account getAccount() {
        return account;
    }

    public LocalDateTime getStatementDate() {
        return statementDate;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public BigDecimal getTotalDebits() {
        return totalDebits;
    }

    public BigDecimal getTotalCredits() {
        return totalCredits;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public LocalDateTime getReconDate() {
        return reconDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    // ========== SETTERS ==========

    public void setAccount(Account account) {
        this.account = account;
    }

    public void setStatementDate(LocalDateTime statementDate) {
        this.statementDate = statementDate;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    public void setClosingBalance(BigDecimal closingBalance) {
        this.closingBalance = closingBalance;
    }

    public void setTotalDebits(BigDecimal totalDebits) {
        this.totalDebits = totalDebits;
    }

    public void setTotalCredits(BigDecimal totalCredits) {
        this.totalCredits = totalCredits;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public void setReconDate(LocalDateTime reconDate) {
        this.reconDate = reconDate;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    // ========== HELPER METHODS ==========

    /**
     * Calculate variance between credits and debits
     */
    public BigDecimal calculateVariance() {
        if (totalCredits == null || totalDebits == null) {
            return BigDecimal.ZERO;
        }
        return totalCredits.subtract(totalDebits);
    }

    /**
     * Check if the statement is balanced
     */
    public boolean isBalanced() {
        if (openingBalance == null || closingBalance == null ||
                totalCredits == null || totalDebits == null) {
            return false;
        }

        BigDecimal calculatedClosing = openingBalance
                .add(totalCredits)
                .subtract(totalDebits);

        return calculatedClosing.compareTo(closingBalance) == 0;
    }

    /**
     * Get statement period as string
     */
    public String getPeriodString() {
        if (periodStart == null || periodEnd == null) {
            return "No period";
        }
        return periodStart + " to " + periodEnd;
    }

    /**
     * Get statement display name
     */
    public String getDisplayName() {
        String accountName = account != null ? account.getName() : "Unknown Account";
        return String.format("Statement for %s - %s",
                accountName,
                statementDate != null ? statementDate.toLocalDate().toString() : "No Date");
    }

    /**
     * Validate required fields
     */
    public boolean isValid() {
        return account != null &&
                statementDate != null &&
                periodStart != null &&
                periodEnd != null &&
                !periodStart.isAfter(periodEnd);
    }

    // ========== BUSINESS LOGIC METHODS ==========

    /**
     * Recalculate closing balance based on opening, credits, and debits
     */
    public void recalculateClosingBalance() {
        if (openingBalance != null && totalCredits != null && totalDebits != null) {
            this.closingBalance = openingBalance
                    .add(totalCredits)
                    .subtract(totalDebits);
        }
    }

    /**
     * Mark as reconciled
     */
    public void markAsReconciled(String reconciledBy) {
        this.reconDate = LocalDateTime.now();
        this.createdBy = reconciledBy;
    }

    /**
     * Check if statement is reconciled
     */
    public boolean isReconciled() {
        return reconDate != null;
    }

    /**
     * Get statement duration in days
     */
    public long getPeriodDurationInDays() {
        if (periodStart == null || periodEnd == null) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd);
    }

    // ========== EQUALS & HASHCODE ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccountStatement that = (AccountStatement) o;

        if (getId() != null) {
            return getId().equals(that.getId());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return getId() != null ? getId().hashCode() : 0;
    }

    // ========== TO STRING ==========

    @Override
    public String toString() {
        return "AccountStatement{" +
                "id=" + getId() +
                ", account=" + (account != null ? account.getId() : "null") +
                ", statementDate=" + statementDate +
                ", period=" + getPeriodString() +
                ", openingBalance=" + openingBalance +
                ", closingBalance=" + closingBalance +
                '}';
    }
}