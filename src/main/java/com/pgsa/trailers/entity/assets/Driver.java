package com.pgsa.trailers.entity.assets;

import com.pgsa.trailers.config.BaseEntity;
import com.pgsa.trailers.enums.DriverStatus;
import com.pgsa.trailers.entity.security.AppUser;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
        name = "driver",
        indexes = {
                @Index(name = "idx_driver_license", columnList = "license_number")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "driver_license_number_key", columnNames = {"license_number"})
        }
)
public class Driver extends BaseEntity {

    // Link to app_user (required: every driver is an app_user)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_driver_app_user"))
    private AppUser appUser;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "license_number", unique = true, nullable = false)
    private String licenseNumber;

    @Column(name = "license_type")
    private String licenseType;

    @Column(name = "license_expiry")
    private LocalDate licenseExpiry;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "driver_status")
    private DriverStatus status;

    // ========== GETTERS ==========

    public AppUser getAppUser() {
        return appUser;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public LocalDate getLicenseExpiry() {
        return licenseExpiry;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public DriverStatus getStatus() {
        return status;
    }

    // ========== SETTERS ==========

    public void setAppUser(AppUser appUser) {
        this.appUser = appUser;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public void setLicenseType(String licenseType) {
        this.licenseType = licenseType;
    }

    public void setLicenseExpiry(LocalDate licenseExpiry) {
        this.licenseExpiry = licenseExpiry;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setStatus(DriverStatus status) {
        this.status = status;
    }

    // ========== HELPER METHODS ==========

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Check if driver's license is expired
     */
    public boolean isLicenseExpired() {
        if (licenseExpiry == null) {
            return false;
        }
        return licenseExpiry.isBefore(LocalDate.now());
    }

    /**
     * Check if license expires within given days
     */
    public boolean isLicenseExpiringWithinDays(int days) {
        if (licenseExpiry == null) {
            return false;
        }
        LocalDate warningDate = LocalDate.now().plusDays(days);
        return !licenseExpiry.isBefore(LocalDate.now()) &&
                !licenseExpiry.isAfter(warningDate);
    }

    /**
     * Check if driver is active
     */
    public boolean isActive() {
        return status == DriverStatus.ACTIVE;
    }

    /**
     * Get years of service
     */
    public Integer getYearsOfService() {
        if (hireDate == null) {
            return null;
        }
        return LocalDate.now().getYear() - hireDate.getYear();
    }

    /**
     * Get driver's age (if date of birth is available in AppUser)
     */
    public Integer getAge() {
        // Assuming AppUser has getDateOfBirth() method
        if (appUser != null) {
            // You would need to add dateOfBirth to AppUser entity
            // LocalDate dateOfBirth = appUser.getDateOfBirth();
            // if (dateOfBirth != null) {
            //     return Period.between(dateOfBirth, LocalDate.now()).getYears();
            // }
        }
        return null;
    }

    /**
     * Validate required fields
     */
    public boolean isValid() {
        return firstName != null && !firstName.trim().isEmpty() &&
                lastName != null && !lastName.trim().isEmpty() &&
                licenseNumber != null && !licenseNumber.trim().isEmpty() &&
                appUser != null;
    }

    /**
     * Get formatted contact information
     */
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

    /**
     * Get driver summary
     */
    public String getSummary() {
        return String.format("%s (%s) - %s",
                getFullName(),
                licenseNumber,
                status != null ? status.name() : "Unknown status");
    }

    // ========== BUSINESS LOGIC METHODS ==========

    /**
     * Activate driver
     */
    public void activate() {
        this.status = DriverStatus.ACTIVE;
    }

    /**
     * Deactivate driver
     */
    public void deactivate(String reason) {
        this.status = DriverStatus.INACTIVE;
        // Could add to audit trail
    }

    /**
     * Check if driver can be assigned to a vehicle
     */
    public boolean canBeAssignedToVehicle() {
        return isActive() &&
                !isLicenseExpired() &&
                status != DriverStatus.SUSPENDED;
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
                ", appUser=" + (appUser != null ? appUser.getId() : "null") +
                '}';
    }

    // ========== BUILDER PATTERN (Optional) ==========

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Driver driver = new Driver();

        public Builder firstName(String firstName) {
            driver.setFirstName(firstName);
            return this;
        }

        public Builder lastName(String lastName) {
            driver.setLastName(lastName);
            return this;
        }

        public Builder licenseNumber(String licenseNumber) {
            driver.setLicenseNumber(licenseNumber);
            return this;
        }

        public Builder appUser(AppUser appUser) {
            driver.setAppUser(appUser);
            return this;
        }

        public Builder status(DriverStatus status) {
            driver.setStatus(status);
            return this;
        }

        public Driver build() {
            return driver;
        }
    }
}