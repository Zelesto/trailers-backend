package com.pgsa.trailers.entity.ops;

import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.assets.Vehicle;
import com.pgsa.trailers.entity.finance.AccountStatement;
import com.pgsa.trailers.config.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "fuel_slip")
public class FuelSlip extends BaseEntity {

    @Column(name = "slip_number", nullable = false, unique = true)
    private String slipNumber;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fuel_source_id")
    private FuelSource fuelSource;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "odometer_reading")
    private BigDecimal odometerReading;

    @Column(name = "location")
    private String location;

    @Column(name = "station_name")
    private String stationName;

    @Column(name = "pump_number")
    private String pumpNumber;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "finalized", nullable = false)
    private Boolean finalized = false;

    @Column(name = "load_id")
    private Long loadId;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "fuel_type")
    private String fuelType;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "receipt_number")
    private String receiptNumber;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "verification_date")
    private LocalDateTime verificationDate;

    @Column(name = "incident_flag")
    private Boolean incidentFlag = false;

    @Column(name = "last_status_update")
    private LocalDateTime lastStatusUpdate;

    @Type(JsonType.class)
    @Column(name = "audit_trail", columnDefinition = "jsonb")
    private Map<String, Object> auditTrail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_statement_id")
    private AccountStatement accountStatement;

    // ========== GETTERS ==========

    public String getSlipNumber() {
        return slipNumber;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Driver getDriver() {
        return driver;
    }

    public FuelSource getFuelSource() {
        return fuelSource;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getOdometerReading() {
        return odometerReading;
    }

    public String getLocation() {
        return location;
    }

    public String getStationName() {
        return stationName;
    }

    public String getPumpNumber() {
        return pumpNumber;
    }

    public String getNotes() {
        return notes;
    }

    public Boolean getFinalized() {
        return finalized;
    }

    public Boolean isFinalized() {
        return finalized;
    }

    public Long getLoadId() {
        return loadId;
    }

    public Long getTripId() {
        return tripId;
    }

    public String getFuelType() {
        return fuelType;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public LocalDateTime getVerificationDate() {
        return verificationDate;
    }

    public Boolean getIncidentFlag() {
        return incidentFlag;
    }

    public Boolean isIncidentFlag() {
        return incidentFlag;
    }

    public LocalDateTime getLastStatusUpdate() {
        return lastStatusUpdate;
    }

    public Map<String, Object> getAuditTrail() {
        return auditTrail;
    }

    public AccountStatement getAccountStatement() {
        return accountStatement;
    }

    // ========== SETTERS ==========

    public void setSlipNumber(String slipNumber) {
        this.slipNumber = slipNumber;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public void setFuelSource(FuelSource fuelSource) {
        this.fuelSource = fuelSource;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setOdometerReading(BigDecimal odometerReading) {
        this.odometerReading = odometerReading;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public void setPumpNumber(String pumpNumber) {
        this.pumpNumber = pumpNumber;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setFinalized(Boolean finalized) {
        this.finalized = finalized;
    }

    public void setLoadId(Long loadId) {
        this.loadId = loadId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public void setVerificationDate(LocalDateTime verificationDate) {
        this.verificationDate = verificationDate;
    }

    public void setIncidentFlag(Boolean incidentFlag) {
        this.incidentFlag = incidentFlag;
    }

    public void setLastStatusUpdate(LocalDateTime lastStatusUpdate) {
        this.lastStatusUpdate = lastStatusUpdate;
    }

    public void setAuditTrail(Map<String, Object> auditTrail) {
        this.auditTrail = auditTrail;
    }

    public void setAccountStatement(AccountStatement accountStatement) {
        this.accountStatement = accountStatement;
    }

    // ========== HELPER METHODS ==========

    /**
     * Calculate total amount if not already set
     */
    public BigDecimal calculateTotalAmount() {
        if (quantity != null && unitPrice != null) {
            return quantity.multiply(unitPrice);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Check if fuel slip can be finalized
     */
    public boolean canBeFinalized() {
        return !Boolean.TRUE.equals(finalized) &&
                slipNumber != null &&
                transactionDate != null &&
                quantity != null &&
                unitPrice != null &&
                totalAmount != null;
    }

    /**
     * Add an entry to the audit trail
     */
    public void addAuditEntry(String action, String performedBy, String details) {
        if (auditTrail != null) {
            // Implementation depends on your audit trail structure
            // This is a basic example
            auditTrail.put("lastAction", action);
            auditTrail.put("lastPerformedBy", performedBy);
            auditTrail.put("lastActionTime", LocalDateTime.now().toString());
            auditTrail.put("details", details);
        }
    }

    /**
     * Get a display name for the fuel slip
     */
    public String getDisplayName() {
        return String.format("Fuel Slip #%s - %s",
                slipNumber,
                transactionDate != null ? transactionDate.toLocalDate().toString() : "No Date");
    }

    /**
     * Validate required fields
     */
    public boolean isValid() {
        return slipNumber != null && !slipNumber.trim().isEmpty() &&
                transactionDate != null &&
                quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0 &&
                unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0 &&
                totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    // ========== EQUALS & HASHCODE ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FuelSlip fuelSlip = (FuelSlip) o;

        if (getId() != null) {
            return getId().equals(fuelSlip.getId());
        }

        return slipNumber != null && slipNumber.equals(fuelSlip.getSlipNumber());
    }

    @Override
    public int hashCode() {
        if (getId() != null) {
            return getId().hashCode();
        }
        return slipNumber != null ? slipNumber.hashCode() : 0;
    }

    // ========== TO STRING ==========

    @Override
    public String toString() {
        return "FuelSlip{" +
                "id=" + getId() +
                ", slipNumber='" + slipNumber + '\'' +
                ", transactionDate=" + transactionDate +
                ", vehicle=" + (vehicle != null ? vehicle.getId() : "null") +
                ", driver=" + (driver != null ? driver.getId() : "null") +
                ", quantity=" + quantity +
                ", totalAmount=" + totalAmount +
                ", finalized=" + finalized +
                '}';
    }
}