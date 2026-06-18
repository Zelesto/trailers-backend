// src/main/java/com/pgsa/trailers/entity/ops/Pod.java
package com.pgsa.trailers.entity.ops;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pods")
public class Pod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pod_number", unique = true, nullable = false, length = 50)
    private String podNumber;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "document_type", length = 50)
    private String documentType;

    @Column(name = "file_size", length = 50)
    private String fileSize;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    public void prePersist() {
        if (podNumber == null) {
            podNumber = generatePodNumber();
        }
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
    }

    private String generatePodNumber() {
        return "POD-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }
}
