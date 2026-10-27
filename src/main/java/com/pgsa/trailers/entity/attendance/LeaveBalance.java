// src/main/java/com/pgsa/trailers/entity/attendance/LeaveBalance.java
package com.pgsa.trailers.entity.attendance;

import com.pgsa.trailers.entity.assets.Driver;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_balances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "\"year\"", nullable = false)
    private Integer year;

    @Column(name = "total_days")
    private BigDecimal totalDays;

    @Column(name = "used_days")
    private BigDecimal usedDays;

    @Column(name = "pending_days")
    private BigDecimal pendingDays;

    @Column(name = "remaining_days")
    private BigDecimal remainingDays;

    @Column(name = "carried_over")
    private BigDecimal carriedOver;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
