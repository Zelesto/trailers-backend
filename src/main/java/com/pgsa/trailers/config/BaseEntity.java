package com.pgsa.trailers.config;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

@Version
    @Column(name = "version")
    private Integer version;

    /**
     * Compatibility note:
     * Several live schemas in this project persist active state via a status column instead of is_active.
     * Keeping this as transient avoids Hibernate schema-validation failures against tables that do not have
     * an is_active column while preserving the in-memory helper methods used throughout the application.
     */
    @Transient
    private Boolean isActive = true;

    /**
     * Soft delete the entity
     */
    public void softDelete() {
        this.isActive = false;
    }

    /**
     * Restore a soft-deleted entity
     */
    public void restore() {
        this.isActive = true;
    }

    /**
     * Check if entity is active
     */
    public boolean isActive() {
        return isActive != null && isActive;
    }

    /**
     * Check if entity is soft-deleted
     */
    public boolean isDeleted() {
        return isActive != null && !isActive;
    }
}
