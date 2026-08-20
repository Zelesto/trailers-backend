package com.pgsa.trailers.entity.assets;

import com.pgsa.trailers.config.BaseEntity;
import com.pgsa.trailers.entity.security.AppUser;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Slf4j
@Table(
        name = "driver",
        indexes = {
                @Index(name = "idx_driver_license", columnList = "license_number"),
                @Index(name = "idx_driver_status", columnList = "status"),
                @Index(name = "idx_driver_app_user", columnList = "app_user_id"),
                @Index(name = "idx_driver_current_status", columnList = "current_status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_driver_license_number", columnNames = {"license_number"}),
                @UniqueConstraint(name = "uk_driver_app_user", columnNames = {"app_user_id"})
        }
)
public class Driver extends BaseEntity {

    // ============================================================
    // CONSTANTS FOR STATUS VALUES (from enum_master table)
    // ============================================================
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_ON_LEAVE = "ON_LEAVE";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_ASSIGNED = "ASSIGNED";
    public static final String STATUS_ON_TRIP = "ON_TRIP";
    public static final String STATUS_CLOCKED_IN = "CLOCKED_IN";
    public static final String STATUS_CLOCKED_OUT = "CLOCKED_OUT";
    public static final String STATUS_ON_BREAK = "ON_BREAK";
    public static final String STATUS_OFF_DUTY = "OFF_DUTY";

    // ====== EXPLICIT LOGGER ======
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Driver.class);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser appUser;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "license_number", unique = true, nullable = false, length = 50)
    private String licenseNumber;

    @Column(name = "license_type", length = 50)
    private String licenseType;

    @Column(name = "license_expiry")
    private LocalDate licenseExpiry;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "termination_reason", length = 255)
    private String terminationReason;

    // ====== ADDITIONAL FIELDS ======
    
    @Column(name = "employment_type", length = 50)
    private String employmentType;

    @Column(name = "shift_pattern", length = 50)
    private String shiftPattern;

    @Column(name = "assigned_vehicle_id")
    private Long assignedVehicleId;

    @Column(name = "training_completed")
    private Boolean trainingCompleted = false;

    @Column(name = "medical_clearance_date")
    private LocalDate medicalClearanceDate;

    @Column(name = "next_medical_due")
    private LocalDate nextMedicalDue;

    @Column(name = "incidents_logged")
    private Integer incidentsLogged = 0;

    @Column(name = "total_trips")
    private Integer totalTrips = 0;

    @Column(name = "total_km_travelled", precision = 12, scale = 2)
    private BigDecimal totalKmTravelled;

    @Column(name = "total_hours_active", precision = 12, scale = 2)
    private BigDecimal totalHoursActive;

    @Column(name = "performance_score", precision = 5, scale = 2)
    private BigDecimal performanceScore;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ====== NEW PUNCH CLOCK FIELDS ======
    @Column(name = "current_status", length = 20)
    private String currentStatus = STATUS_OFF_DUTY;

    @Column(name = "last_clock_in")
    private LocalDateTime lastClockIn;

    @Column(name = "last_clock_out")
    private LocalDateTime lastClockOut;

    @Column(name = "last_trip_date")
    private LocalDate lastTripDate;

    // ====== NEW PERSONAL INFORMATION FIELDS ======
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "emergency_contact_name", length = 200)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 50)
    private String emergencyContactPhone;

    @Column(name = "bank_name", length = 200)
    private String bankName;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_branch_code", length = 20)
    private String bankBranchCode;

    @Column(name = "tax_number", length = 50)
    private String taxNumber;

    @Column(name = "last_medical_exam_date")
    private LocalDate lastMedicalExamDate;

    @Column(name = "next_medical_exam_date")
    private LocalDate nextMedicalExamDate;

    @Column(name = "driver_license_class", length = 50)
    private String driverLicenseClass;

    @Column(name = "license_issue_date")
    private LocalDate licenseIssueDate;

    @Column(name = "license_restrictions", columnDefinition = "TEXT")
    private String licenseRestrictions;

    @Column(name = "endorsements", columnDefinition = "TEXT")
    private String endorsements;

    @Column(name = "driver_photo_url", length = 500)
    private String driverPhotoUrl;

    @Column(name = "employee_id", length = 50)
    private String employeeId;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "supervisor_id")
    private Long supervisorId;

    // ====== AUDIT TRAIL ======
    @Type(JsonType.class)
    @Column(name = "audit_trail", columnDefinition = "jsonb")
    private Map<String, Object> auditTrail = new HashMap<>();

    // ========== CONSTRUCTORS ==========

    public Driver() {
        this.status = STATUS_ACTIVE;
        this.incidentsLogged = 0;
        this.totalTrips = 0;
        this.trainingCompleted = false;
        this.currentStatus = STATUS_OFF_DUTY;
        this.auditTrail = new HashMap<>();
        this.setIsActive(true);
        this.setVersion(0);
    }

    // ========== HELPER METHODS ==========

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isLicenseExpired() {
        if (licenseExpiry == null) {
            return false;
        }
        return licenseExpiry.isBefore(LocalDate.now());
    }

    public boolean isLicenseExpiringWithinDays(int days) {
        if (licenseExpiry == null) {
            return false;
        }
        LocalDate warningDate = LocalDate.now().plusDays(days);
        return !licenseExpiry.isBefore(LocalDate.now()) &&
                !licenseExpiry.isAfter(warningDate);
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status) && super.isActive();
    }

    public boolean isAvailableForAssignment() {
        return isActive() && 
               !isLicenseExpired() && 
               !STATUS_SUSPENDED.equals(status) &&
               !STATUS_ON_LEAVE.equals(status) &&
               assignedVehicleId == null;
    }

    public Integer getYearsOfService() {
        if (hireDate == null) {
            return null;
        }
        return Period.between(hireDate, LocalDate.now()).getYears();
    }

    public Integer getAge() {
        if (dateOfBirth == null) {
            return null;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public boolean isValid() {
        return firstName != null && !firstName.trim().isEmpty() &&
                lastName != null && !lastName.trim().isEmpty() &&
                licenseNumber != null && !licenseNumber.trim().isEmpty() &&
                appUser != null;
    }

    public String getContactInfo() {
        StringBuilder sb = new StringBuilder();
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            sb.append("Phone: ").append(phoneNumber);
        }
        if (email != null && !email.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append("Email: ").append(email);
        }
        return sb.length() > 0 ? sb.toString() : "No contact info";
    }

    public String getSummary() {
        return String.format("%s (%s) - %s",
                getFullName(),
                licenseNumber,
                status != null ? status : "Unknown status");
    }

    // ========== PUNCH CLOCK METHODS ==========

    public boolean isClockedIn() {
        return STATUS_CLOCKED_IN.equals(currentStatus) || STATUS_ON_BREAK.equals(currentStatus);
    }

    public boolean isOnBreak() {
        return STATUS_ON_BREAK.equals(currentStatus);
    }

    public boolean isOffDuty() {
        return STATUS_OFF_DUTY.equals(currentStatus);
    }

    public void clockIn() {
        this.currentStatus = STATUS_CLOCKED_IN;
        this.lastClockIn = LocalDateTime.now();
    }

    public void startBreak() {
        this.currentStatus = STATUS_ON_BREAK;
    }

    public void endBreak() {
        this.currentStatus = STATUS_CLOCKED_IN;
    }

    public void clockOut() {
        this.currentStatus = STATUS_OFF_DUTY;
        this.lastClockOut = LocalDateTime.now();
    }

    // ========== BUSINESS LOGIC METHODS - FIXED ==========

    public void activate() {
        this.status = STATUS_ACTIVE;
        this.terminationDate = null;
        this.terminationReason = null;
        this.setIsActive(true);
        this.currentStatus = STATUS_OFF_DUTY;
    }

    public void deactivate(String reason) {
        this.status = STATUS_INACTIVE;
        this.terminationDate = LocalDate.now();
        this.terminationReason = reason;
        this.setIsActive(false);
        this.currentStatus = STATUS_OFF_DUTY;
    }

    public void suspend(String reason) {
        this.status = STATUS_SUSPENDED;
        this.terminationReason = reason;
        this.currentStatus = STATUS_OFF_DUTY;
    }

    public void reinstate() {
        this.status = STATUS_ACTIVE;
        this.terminationReason = null;
        this.setIsActive(true);
        this.currentStatus = STATUS_OFF_DUTY;
    }

    public void setOnLeave() {
        this.status = STATUS_ON_LEAVE;
        this.currentStatus = STATUS_OFF_DUTY;
    }

    public boolean canBeAssigned() {
        return isActive() &&
                !isLicenseExpired() &&
                !STATUS_SUSPENDED.equals(status) &&
                !STATUS_ON_LEAVE.equals(status) &&
                assignedVehicleId == null &&
                !isClockedIn();
    }

    public void assignVehicle(Long vehicleId) {
        this.assignedVehicleId = vehicleId;
    }

    public void unassignVehicle() {
        this.assignedVehicleId = null;
    }

    public void incrementTrips() {
        this.totalTrips = (totalTrips == null ? 0 : totalTrips) + 1;
        this.lastTripDate = LocalDate.now();
    }

    public void incrementIncidents() {
        this.incidentsLogged = (incidentsLogged == null ? 0 : incidentsLogged) + 1;
    }

    public void addKilometers(BigDecimal km) {
        if (km != null) {
            if (this.totalKmTravelled == null) {
                this.totalKmTravelled = BigDecimal.ZERO;
            }
            this.totalKmTravelled = this.totalKmTravelled.add(km);
        }
    }

    public void addActiveHours(BigDecimal hours) {
        if (hours != null) {
            if (this.totalHoursActive == null) {
                this.totalHoursActive = BigDecimal.ZERO;
            }
            this.totalHoursActive = this.totalHoursActive.add(hours);
        }
    }

    public boolean isMedicalClearanceValid() {
        return nextMedicalDue == null || !nextMedicalDue.isBefore(LocalDate.now());
    }

    public boolean isMedicalClearanceExpiringWithinDays(int days) {
        if (nextMedicalDue == null) {
            return false;
        }
        LocalDate warningDate = LocalDate.now().plusDays(days);
        return !nextMedicalDue.isBefore(LocalDate.now()) &&
                !nextMedicalDue.isAfter(warningDate);
    }

    // ========== EXPLICIT GETTERS AND SETTERS ==========

    // --- BaseEntity methods ---
    public Boolean getIsActive() {
        return super.isActive();
    }

    public void setIsActive(Boolean isActive) {
        super.setIsActive(isActive);
    }

    public Integer getVersion() {
        return super.getVersion();
    }

    public void setVersion(Integer version) {
        super.setVersion(version);
    }

    public Long getId() {
        return super.getId();
    }

    // --- AppUser ---
    public AppUser getAppUser() {
        return appUser;
    }

    public void setAppUser(AppUser appUser) {
        this.appUser = appUser;
    }

    // --- First Name ---
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    // --- Last Name ---
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    // --- License Number ---
    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    // --- License Type ---
    public String getLicenseType() {
        return licenseType;
    }

    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    // --- License Expiry ---
    public LocalDate getLicenseExpiry() {
        return licenseExpiry;
    }

    public void setLicenseExpiry(LocalDate licenseExpiry) {
        this.licenseExpiry = licenseExpiry;
    }

    // --- Hire Date ---
    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    // --- Phone Number ---
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    // --- Email ---
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // --- Status ---
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }

    // --- Termination Date ---
    public LocalDate getTerminationDate() {
        return terminationDate;
    }

    public void setTerminationDate(LocalDate terminationDate) {
        this.terminationDate = terminationDate;
    }

    // --- Termination Reason ---
    public String getTerminationReason() {
        return terminationReason;
    }

    public void setTerminationReason(String terminationReason) {
        this.terminationReason = terminationReason;
    }

    // --- Employment Type ---
    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    // --- Shift Pattern ---
    public String getShiftPattern() {
        return shiftPattern;
    }

    public void setShiftPattern(String shiftPattern) {
        this.shiftPattern = shiftPattern;
    }

    // --- Assigned Vehicle ID ---
    public Long getAssignedVehicleId() {
        return assignedVehicleId;
    }

    public void setAssignedVehicleId(Long assignedVehicleId) {
        this.assignedVehicleId = assignedVehicleId;
    }

    // --- Training Completed ---
    public Boolean getTrainingCompleted() {
        return trainingCompleted;
    }

    public void setTrainingCompleted(Boolean trainingCompleted) {
        this.trainingCompleted = trainingCompleted;
    }

    // --- Medical Clearance Date ---
    public LocalDate getMedicalClearanceDate() {
        return medicalClearanceDate;
    }

    public void setMedicalClearanceDate(LocalDate medicalClearanceDate) {
        this.medicalClearanceDate = medicalClearanceDate;
    }

    // --- Next Medical Due ---
    public LocalDate getNextMedicalDue() {
        return nextMedicalDue;
    }

    public void setNextMedicalDue(LocalDate nextMedicalDue) {
        this.nextMedicalDue = nextMedicalDue;
    }

    // --- Incidents Logged ---
    public Integer getIncidentsLogged() {
        return incidentsLogged;
    }

    public void setIncidentsLogged(Integer incidentsLogged) {
        this.incidentsLogged = incidentsLogged;
    }

    // --- Total Trips ---
    public Integer getTotalTrips() {
        return totalTrips;
    }

    public void setTotalTrips(Integer totalTrips) {
        this.totalTrips = totalTrips;
    }

    // --- Total Km Travelled ---
    public BigDecimal getTotalKmTravelled() {
        return totalKmTravelled;
    }

    public void setTotalKmTravelled(BigDecimal totalKmTravelled) {
        this.totalKmTravelled = totalKmTravelled;
    }

    // --- Total Hours Active ---
    public BigDecimal getTotalHoursActive() {
        return totalHoursActive;
    }

    public void setTotalHoursActive(BigDecimal totalHoursActive) {
        this.totalHoursActive = totalHoursActive;
    }

    // --- Performance Score ---
    public BigDecimal getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(BigDecimal performanceScore) {
        this.performanceScore = performanceScore;
    }

    // --- Notes ---
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // --- Current Status ---
    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    // --- Last Clock In ---
    public LocalDateTime getLastClockIn() {
        return lastClockIn;
    }

    public void setLastClockIn(LocalDateTime lastClockIn) {
        this.lastClockIn = lastClockIn;
    }

    // --- Last Clock Out ---
    public LocalDateTime getLastClockOut() {
        return lastClockOut;
    }

    public void setLastClockOut(LocalDateTime lastClockOut) {
        this.lastClockOut = lastClockOut;
    }

    // --- Last Trip Date ---
    public LocalDate getLastTripDate() {
        return lastTripDate;
    }

    public void setLastTripDate(LocalDate lastTripDate) {
        this.lastTripDate = lastTripDate;
    }

    // --- Date of Birth ---
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    // --- Gender ---
    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    // --- Country ---
    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    // --- Address ---
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    // --- Emergency Contact Name ---
    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    // --- Emergency Contact Phone ---
    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    // --- Bank Name ---
    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    // --- Bank Account Number ---
    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    // --- Bank Branch Code ---
    public String getBankBranchCode() {
        return bankBranchCode;
    }

    public void setBankBranchCode(String bankBranchCode) {
        this.bankBranchCode = bankBranchCode;
    }

    // --- Tax Number ---
    public String getTaxNumber() {
        return taxNumber;
    }

    public void setTaxNumber(String taxNumber) {
        this.taxNumber = taxNumber;
    }

    // --- Last Medical Exam Date ---
    public LocalDate getLastMedicalExamDate() {
        return lastMedicalExamDate;
    }

    public void setLastMedicalExamDate(LocalDate lastMedicalExamDate) {
        this.lastMedicalExamDate = lastMedicalExamDate;
    }

    // --- Next Medical Exam Date ---
    public LocalDate getNextMedicalExamDate() {
        return nextMedicalExamDate;
    }

    public void setNextMedicalExamDate(LocalDate nextMedicalExamDate) {
        this.nextMedicalExamDate = nextMedicalExamDate;
    }

    // --- Driver License Class ---
    public String getDriverLicenseClass() {
        return driverLicenseClass;
    }

    public void setDriverLicenseClass(String driverLicenseClass) {
        this.driverLicenseClass = driverLicenseClass;
    }

    // --- License Issue Date ---
    public LocalDate getLicenseIssueDate() {
        return licenseIssueDate;
    }

    public void setLicenseIssueDate(LocalDate licenseIssueDate) {
        this.licenseIssueDate = licenseIssueDate;
    }

    // --- License Restrictions ---
    public String getLicenseRestrictions() {
        return licenseRestrictions;
    }

    public void setLicenseRestrictions(String licenseRestrictions) {
        this.licenseRestrictions = licenseRestrictions;
    }

    // --- Endorsements ---
    public String getEndorsements() {
        return endorsements;
    }

    public void setEndorsements(String endorsements) {
        this.endorsements = endorsements;
    }

    // --- Driver Photo URL ---
    public String getDriverPhotoUrl() {
        return driverPhotoUrl;
    }

    public void setDriverPhotoUrl(String driverPhotoUrl) {
        this.driverPhotoUrl = driverPhotoUrl;
    }

    // --- Employee ID ---
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    // --- Department ---
    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    // --- Supervisor ID ---
    public Long getSupervisorId() {
        return supervisorId;
    }

    public void setSupervisorId(Long supervisorId) {
        this.supervisorId = supervisorId;
    }

    // --- Audit Trail ---
    public Map<String, Object> getAuditTrail() {
        return auditTrail;
    }

    public void setAuditTrail(Map<String, Object> auditTrail) {
        this.auditTrail = auditTrail;
    }

    // ========== EQUALS & HASHCODE ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Driver driver = (Driver) o;

        if (getId() != null) {
            return getId().equals(driver.getId());
        }

        return licenseNumber != null && licenseNumber.equals(driver.getLicenseNumber());
    }

    @Override
    public int hashCode() {
        if (getId() != null) {
            return getId().hashCode();
        }
        return licenseNumber != null ? licenseNumber.hashCode() : 0;
    }

    // ========== TO STRING ==========

    @Override
    public String toString() {
        return "Driver{" +
                "id=" + getId() +
                ", fullName='" + getFullName() + '\'' +
                ", licenseNumber='" + licenseNumber + '\'' +
                ", status=" + status +
                ", currentStatus='" + currentStatus + '\'' +
                ", appUserId=" + (appUser != null ? appUser.getId() : "null") +
                ", isActive=" + getIsActive() +
                '}';
    }

    // ========== LIFECYCLE HOOKS ==========

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = STATUS_ACTIVE;
        }
        if (incidentsLogged == null) {
            incidentsLogged = 0;
        }
        if (totalTrips == null) {
            totalTrips = 0;
        }
        if (trainingCompleted == null) {
            trainingCompleted = false;
        }
        if (currentStatus == null) {
            currentStatus = STATUS_OFF_DUTY;
        }
        if (auditTrail == null) {
            auditTrail = new HashMap<>();
        }
        if (getIsActive() == null) {
            setIsActive(true);
        }
        if (getVersion() == null) {
            setVersion(0);
        }
        log.debug("✅ Driver pre-persist: {}", getFullName());
    }

    @PreUpdate
    protected void onUpdate() {
        log.debug("🔄 Driver pre-update: {}", getFullName());
    }
}
