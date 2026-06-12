package com.pgsa.trailers.entity.security;

import com.pgsa.trailers.entity.security.AuditedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version")
    private Integer version;

    @Column(name = "is_active", nullable = false)
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
