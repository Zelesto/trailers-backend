package com.pgsa.trailers.service.finance;

import com.pgsa.trailers.entity.finance.Receivable;
import com.pgsa.trailers.entity.finance.ReceivableStatus;
import com.pgsa.trailers.repository.finance.ReceivableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceivableService {

    private final ReceivableRepository receivableRepository;

    @Transactional
    public Receivable createReceivable(Receivable receivable) {
        if (receivable.getReceivableNumber() == null) {
            receivable.setReceivableNumber(generateReceivableNumber());
        }
        if (receivable.getStatus() == null) {
            receivable.setStatus(ReceivableStatus.PENDING);
        }
        if (receivable.getBalanceDue() == null) {
            receivable.setBalanceDue(receivable.getOriginalAmount());
        }
        receivable.setCreatedAt(LocalDateTime.now());
        receivable.setUpdatedAt(LocalDateTime.now());
        
        return receivableRepository.save(receivable);
    }

    @Transactional
    public Receivable recordPayment(Long receivableId, BigDecimal paymentAmount) {
        Receivable receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new RuntimeException("Receivable not found"));

        BigDecimal newPaidAmount = receivable.getAmountPaid().add(paymentAmount);
        receivable.setAmountPaid(newPaidAmount);
        
        BigDecimal newBalance = receivable.getOriginalAmount().subtract(newPaidAmount);
        receivable.setBalanceDue(newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            receivable.setStatus(ReceivableStatus.PAID);
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            receivable.setStatus(ReceivableStatus.PARTIAL);
        }

        receivable.setUpdatedAt(LocalDateTime.now());
        return receivableRepository.save(receivable);
    }

    @Transactional
    public void markAsOverdue(Long receivableId) {
        Receivable receivable = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new RuntimeException("Receivable not found"));
        
        if (receivable.getDueDate().isBefore(LocalDate.now()) && 
            receivable.getStatus() != ReceivableStatus.PAID) {
            receivable.setStatus(ReceivableStatus.OVERDUE);
            receivable.setUpdatedAt(LocalDateTime.now());
            receivableRepository.save(receivable);
        }
    }

    private String generateReceivableNumber() {
        return "REC-" + LocalDate.now().getYear() + "-" + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
