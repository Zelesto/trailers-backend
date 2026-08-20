package com.pgsa.trailers.entity.finance;

import com.pgsa.trailers.config.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "payment")
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false)
    private BigDecimal amount;

    private String reference;

    @Column(nullable = false)
    private String status;

    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
